package net.yacy.document.parser.rdfa;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;

public class RDFaRefiner {
    public static byte[] refine(byte[] bytesHtmlInput) {
        Document domDocument = Jsoup.parse(new String(bytesHtmlInput));

        Elements rdfaPropertyHtmlTags = domDocument.getElementsByAttribute("property");

        for (Element rdfaPropertyHtmlTag: rdfaPropertyHtmlTags) {
            if (rdfaPropertyHtmlTag.children().hasAttr("property")) {
                continue;
            }

            if (rdfaPropertyHtmlTag.hasAttr("content")) {
                continue;
            }

            rdfaPropertyHtmlTag.attr("content", Jsoup.clean(rdfaPropertyHtmlTag.html(), createSafelist()));
        }

        String refinedHtmlDocument = domDocument.outerHtml();

        return refinedHtmlDocument.getBytes();
    }

    private static Safelist createSafelist() {
        return new Safelist()
            .addTags("a", "b", "br", "em", "h1", "h2", "h3", "h4", "h5", "h6", "i", "li", "ol", "p", "strong", "sub",
                "sup", "ul")
            .addAttributes("a", "href")
            .addProtocols("a", "href", "ftp", "http", "https", "mailto");
    }
}
