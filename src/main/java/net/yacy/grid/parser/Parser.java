/**
 *  Parser
 *  Copyright 1.04.2017 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package net.yacy.grid.parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import javax.servlet.Servlet;

import org.json.JSONArray;
import org.json.JSONObject;

import ai.susi.mind.SusiAction;
import ai.susi.mind.SusiThought;
import net.yacy.document.LibraryProvider;
import net.yacy.document.parser.pdfParser;
import net.yacy.grid.YaCyServices;
import net.yacy.grid.io.assets.Asset;
import net.yacy.grid.io.index.CrawlerDocument;
import net.yacy.grid.io.index.CrawlerDocument.Status;
import net.yacy.grid.io.index.CrawlerMapping;
import net.yacy.grid.io.index.WebMapping;
import net.yacy.grid.mcp.AbstractBrokerListener;
import net.yacy.grid.mcp.BrokerListener;
import net.yacy.grid.mcp.Data;
import net.yacy.grid.mcp.MCP;
import net.yacy.grid.mcp.Service;
import net.yacy.grid.parser.api.JSONLDValidatorService;
import net.yacy.grid.parser.api.ParserService;
import net.yacy.grid.tools.DateParser;
import net.yacy.grid.tools.Digest;
import net.yacy.grid.tools.GitTool;
import net.yacy.grid.tools.JSONList;
import net.yacy.grid.tools.Logger;
import net.yacy.grid.tools.Memory;

public class Parser {

    private final static YaCyServices PARSER_SERVICE = YaCyServices.parser;
    private final static String DATA_PATH = "data";
    private final static String LIBRARY_PATH = "conf/libraries/";

    // define services
    @SuppressWarnings("unchecked")
    public final static Class<? extends Servlet>[] PARSER_SERVICES = new Class[]{
            // information services
            ParserService.class,
            JSONLDValidatorService.class
    };

    /*
     * test this with
     * curl -X POST -F "message=@job.json" -F "serviceName=parser" -F "queueName=yacyparser" http://yacygrid.com:8100/yacy/grid/mcp/messages/send.json
{
  "metadata": {
    "process": "yacy_grid_parser",
    "count": 1
  },
  "data": [{"collection": "test"}],
  "actions": [{
    "type": "parser",
    "queue": "yacyparser",
    "sourceasset": "test3/yacy.net.warc.gz",
    "targetasset": "test3/yacy.net.text.jsonlist",
    "targetgraph": "test3/yacy.net.graph.jsonlist",
    "actions": [
      {
      "type": "indexer",
      "queue": "elasticsearch",
      "targetindex": "webindex",
      "targettype" : "common",
      "sourceasset": "test3/yacy.net.text.jsonlist"
      },
      {
        "type": "crawler",
        "queue": "webcrawler",
        "sourceasset": "test3/yacy.net.graph.jsonlist"
      }
    ]
  }]
}
     */
    public static class ParserListener extends AbstractBrokerListener implements BrokerListener {

        public ParserListener(final YaCyServices service) {
             super(service, Runtime.getRuntime().availableProcessors());
        }

        @Override
        public ActionResult processAction(final SusiAction action, final JSONArray data, final String processName, final int processNumber) {

            // check short memory status
            if (Memory.shortStatus()) {
                pdfParser.clean_up_idiotic_PDFParser_font_cache_which_eats_up_tons_of_megabytes();
            }

            final String sourceasset_path = action.getStringAttr("sourceasset");
            final String targetasset_path = action.getStringAttr("targetasset");
            final String targetgraph_path = action.getStringAttr("targetgraph");
            if (targetasset_path == null || targetasset_path.length() == 0 ||
                sourceasset_path == null || sourceasset_path.length() == 0) return ActionResult.FAIL_IRREVERSIBLE;

            byte[] source = null;
            if (action.hasAsset(sourceasset_path)) {
            	source = action.getBinaryAsset(sourceasset_path);
            }
            if (source == null) try {
                final Asset<byte[]> asset = Data.gridStorage.load(sourceasset_path);
                source = asset.getPayload();
            } catch (final Throwable e) {
                Logger.warn("Parser.processAction", e);
                // if we do not get the payload from the storage, we look for attached data in the action
                Logger.warn("Parser.processAction could not load asset: " + sourceasset_path, e);
                return ActionResult.FAIL_IRREVERSIBLE;
            }
            try{
                InputStream sourceStream = null;
                sourceStream = new ByteArrayInputStream(source);
                if (sourceasset_path.endsWith(".gz")) try {
                    sourceStream = new GZIPInputStream(sourceStream);
                } catch (final ZipException e) {
                    // This may actually not be in gzip format in case that a http process unzipped it already.
                    // In that case we simply ignore the exception and the sourcestream stays as it is
                }

                // compute parsed documents
                final String crawlid = action.getStringAttr("id");
                final JSONObject crawl = SusiThought.selectData(data, "id", crawlid);
                final Map<String, Pattern> collections = WebMapping.collectionParser(crawl.optString("collection"));
                final JSONArray parsedDocuments = ParserService.indexWarcRecords(sourceStream, collections);
                final JSONList targetasset_object = new JSONList();
                final JSONList targetgraph_object = new JSONList();
                for (int i = 0; i < parsedDocuments.length(); i++) {
                    final JSONObject docjson = parsedDocuments.getJSONObject(i);
                    final String url = docjson.getString(WebMapping.url_s.name());

                    // create elasticsearch index line
                    final String urlid = Digest.encodeMD5Hex(url);
                    final JSONObject bulkjson = new JSONObject().put("index", new JSONObject().put("_id", urlid));

                    // omit documents which have a canonical tag and are not self-addressed canonical documents
                    boolean is_canonical = true;
                    final String canonical_url = docjson.optString(WebMapping.canonical_s.name());
                    if (canonical_url.length() > 0 && !url.equals(canonical_url)) is_canonical = false;

                    final JSONObject updater = new JSONObject();
                    updater.put(CrawlerMapping.status_date_dt.getMapping().name(), DateParser.iso8601MillisFormat.format(new Date()));
                    if (is_canonical) {
                        // write web index document for canonical documents
                        targetasset_object.add(bulkjson);
                        //docjson.put("_id", id);
                        targetasset_object.add(docjson);
                        // put success into crawler index
                        updater
                            .put(CrawlerMapping.status_s.getMapping().name(), Status.parsed.name())
                            .put(CrawlerMapping.comment_t.getMapping().name(), docjson.optString(WebMapping.title.getMapping().name()));
                    } else {
                        // for non-canonical documents we suppress indexing and write to crawler index only
                        updater
                            .put(CrawlerMapping.status_s.getMapping().name(), Status.noncanonical.name())
                            .put(CrawlerMapping.comment_t.getMapping().name(), docjson.optString("omitted, canonical: " + canonical_url));
                    }

                    // write crawler index
                    try {
                        CrawlerDocument.update(Data.gridIndex, urlid, updater);
                        // check with http://localhost:9200/crawler/_search?q=status_s:parsed
                    } catch (final IOException e) {
                        // well that should not happen
                        Logger.warn("could not write crawler index", e);
                    }

                    // write graph document
                    if (targetgraph_object != null) {
                        targetgraph_object.add(bulkjson);
                        final JSONObject graphjson = ParserService.extractGraph(docjson);
                        //graphjson.put("_id", id);
                        targetgraph_object.add(graphjson);
                    }
                }

                boolean storeToMessage = true; // debug version for now: always true TODO: set to false later
                if (!storeToMessage) {
                    try {
                        final String targetasset = targetasset_object.toString();
                        Data.gridStorage.store(targetasset_path, targetasset.getBytes(StandardCharsets.UTF_8));
                        Logger.info("Parser.processAction stored asset " + targetasset_path);
                    } catch (final Throwable ee) {
                        Logger.warn("Parser.processAction asset " + targetasset_path + " could not be stored, carrying the asset within the next action", ee);
                        storeToMessage = true;
                    }
                    try {
                        final String targetgraph = targetgraph_object.toString();
                        Data.gridStorage.store(targetgraph_path, targetgraph.getBytes(StandardCharsets.UTF_8));
                        Logger.info("Parser.processAction stored graph " + targetgraph_path);
                    } catch (final Throwable ee) {
                        Logger.warn("Parser.processAction asset " + targetgraph_path + " could not be stored, carrying the asset within the next action", ee);
                        storeToMessage = true;
                    }
                }
                // emergency storage to message
                if (storeToMessage) {
                    final JSONArray actions = action.getEmbeddedActions();
                    actions.forEach(a -> {
                        new SusiAction((JSONObject) a).setJSONListAsset(targetasset_path, targetasset_object);
                        new SusiAction((JSONObject) a).setJSONListAsset(targetgraph_path, targetgraph_object);
                        Logger.info("Parser.processAction stored assets " + targetasset_path + ", " + targetgraph_path + " into message");
                    });
                }
                Logger.info("Parser.processAction processed message from queue and stored asset " + targetasset_path);

                return ActionResult.SUCCESS;
            } catch (final Throwable e) {
                Logger.warn("", e);
                return ActionResult.FAIL_IRREVERSIBLE;
            }
        }
    }

    public static void main(final String[] args) {
        System.getProperties().put("jdk.xml.totalEntitySizeLimit", "0");
        System.getProperties().put("jdk.xml.entityExpansionLimit", "0");

        // Initialize Libraries
        new Thread("LibraryProvider.initialize") {
            @Override
            public void run() {
                LibraryProvider.initialize(new File(LIBRARY_PATH));
            }
        }.start();

        // initialize environment variables
        final List<Class<? extends Servlet>> services = new ArrayList<>();
        services.addAll(Arrays.asList(MCP.MCP_SERVICES));
        services.addAll(Arrays.asList(PARSER_SERVICES));
        Service.initEnvironment(PARSER_SERVICE, services, DATA_PATH, false);

        // start listener
        final BrokerListener brokerListener = new ParserListener(PARSER_SERVICE);
        new Thread(brokerListener).start();

        // start server
        Logger.info("started Parser");
        Logger.info(new GitTool().toString());
        Service.runService(null);
        brokerListener.terminate();
    }

}
