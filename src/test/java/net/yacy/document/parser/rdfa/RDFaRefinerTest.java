package net.yacy.document.parser.rdfa;

import org.eclipse.rdf4j.query.algebra.Str;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RDFaRefinerTest {

    @Test
    void fill_content_attribute() {
        String bytesHtmlInput = buildCompleteHtml(
            "<h2 property=\"leistungsbezeichnung\">" +
                "<b>Kfz-Schein Änderung</b>" +
            "</h2>"
        );


        byte[] bytesHtmlInputRefined = RDFaRefiner.refine(bytesHtmlInput.getBytes());


        String bytesHtmlRefinedExpected = buildCompleteHtml(
            "<h2 property=\"leistungsbezeichnung\" content=\"<b>Kfz-Schein Änderung</b>\">" +
                "<b>Kfz-Schein Änderung</b>" +
            "</h2>"
        );
        assertEqualsExpectedWithRefined(bytesHtmlRefinedExpected, bytesHtmlInputRefined);
    }

    @Test
    void filter_not_allowed_xzufi_tags_inclusive_content() {
        String bytesHtmlInput = buildCompleteHtml(
            "<h2 property=\"leistungsbezeichnung\">" +
                "<b>Kfz-Schein Änderung</b>" +
                "<script>alert('I\'m malicious code')</script>" +
            "</h2>"
        );


        byte[] bytesHtmlInputRefined = RDFaRefiner.refine(bytesHtmlInput.getBytes());


        String bytesHtmlRefinedExpected = buildCompleteHtml(
            "<h2 property=\"leistungsbezeichnung\" content=\"<b>Kfz-Schein Änderung</b>\">" +
                "<b>Kfz-Schein Änderung</b>" +
                "<script>alert('I\'m malicious code')</script>" +
            "</h2>"
        );
        assertEqualsExpectedWithRefined(bytesHtmlRefinedExpected, bytesHtmlInputRefined);
    }

    @Test
    void do_not_replace_already_set_content_attribute() {
        String bytesHtmlInput = buildCompleteHtml(
            "<h2 property=\"leistungsbezeichnung\" content=\"<b>Namensänderung</b>\">" +
                "<b>Kfz-Schein Änderung</b>" +
            "</h2>"
        );


        byte[] bytesHtmlInputRefined = RDFaRefiner.refine(bytesHtmlInput.getBytes());


        String bytesHtmlRefinedExpected = buildCompleteHtml(
            "<h2 property=\"leistungsbezeichnung\" content=\"<b>Namensänderung</b>\">" +
                "<b>Kfz-Schein Änderung</b>" +
            "</h2>"
        );
        assertEqualsExpectedWithRefined(bytesHtmlRefinedExpected, bytesHtmlInputRefined);
    }

    @Test
    void do_not_replace_already_set_content_attribute_with_not_allowed_xzufi_tags() {
        String bytesHtmlInput = buildCompleteHtml(
            "<h2 property=\"leistungsbezeichnung\" content=\"" +
                "<b>Namensänderung</b>" +
                "<script>alert('I\'m malicious code')</script>" +
            "\">" +
                "<b>Kfz-Schein Änderung</b>" +
            "</h2>"
        );


        byte[] bytesHtmlInputRefined = RDFaRefiner.refine(bytesHtmlInput.getBytes());


        String bytesHtmlRefinedExpected = buildCompleteHtml(
            "<h2 property=\"leistungsbezeichnung\" content=\"" +
                "<b>Namensänderung</b>" +
                "<script>alert('I\'m malicious code')</script>" +
            "\">" +
                "<b>Kfz-Schein Änderung</b>" +
            "</h2>"
        );
        assertEqualsExpectedWithRefined(bytesHtmlRefinedExpected, bytesHtmlInputRefined);
    }

    @Test
    void allow_xzufi_tags() {
        String bytesHtmlInput = buildCompleteHtml(
            "<div property=\"erforderlicheUnterlagen\">" +
                allowedTagsExample() +
            "</div>"
        );


        byte[] bytesHtmlInputRefined = RDFaRefiner.refine(bytesHtmlInput.getBytes());


        String bytesHtmlRefinedExpected = buildCompleteHtml(
            "<div property=\"erforderlicheUnterlagen\" content=\"" + allowedTagsExample().replaceAll("\"", "&quot;") + "\">" +
                allowedTagsExample() +
            "</div>"
        );
        assertEqualsExpectedWithRefined(bytesHtmlRefinedExpected, bytesHtmlInputRefined);
    }

    @Test
    void repair_invalid_html() {
        String bytesHtmlInput = buildCompleteHtml(
            "<h2 property=\"leistungsbezeichnung\">" +
                "<b>Kfz-Schein Änderung" +
            "</h2>"
        );


        byte[] bytesHtmlInputRefined = RDFaRefiner.refine(bytesHtmlInput.getBytes());


        String bytesHtmlRefinedExpected = buildCompleteHtml(
            "<h2 property=\"leistungsbezeichnung\" content=\"<b>Kfz-Schein Änderung</b>\">" +
                "<b>Kfz-Schein Änderung</b>" +
            "</h2>"
        );
        assertEqualsExpectedWithRefined(bytesHtmlRefinedExpected, bytesHtmlInputRefined);
    }

    @Test
    void do_nothing_if_content_attribute_is_set_without_property_attribute() {
        String bytesHtmlInput = buildCompleteHtml(
            "<h2 content=\"<b>Namensänderung</b>\">" +
                "<b>Kfz-Schein Änderung</b>" +
            "</h2>"
        );


        byte[] bytesHtmlInputRefined = RDFaRefiner.refine(bytesHtmlInput.getBytes());


        String bytesHtmlRefinedExpected = buildCompleteHtml(
            "<h2 content=\"<b>Namensänderung</b>\">" +
                "<b>Kfz-Schein Änderung</b>" +
            "</h2>"
        );
        assertEqualsExpectedWithRefined(bytesHtmlRefinedExpected, bytesHtmlInputRefined);
    }

    @Test
    void ignore_tag_without_property_attribute_in_parent_with_property_attribute_and_sibling_with_property_attribute() {
        String bytesHtmlInput = buildCompleteHtml(
            "<h2 property=\"erforderlicheUnterlagen\">" +
                "<span>" +
                    "<b>Kfz-Schein Änderung Teil 1</b>" +
                "</span>" +
                "<span property=\"unterlage1\">" +
                    "<b>Kfz-Schein Änderung Teil 2</b>" +
                "</span>" +
            "</h2>"
        );


        byte[] bytesHtmlInputRefined = RDFaRefiner.refine(bytesHtmlInput.getBytes());


        String bytesHtmlRefinedExpected = buildCompleteHtml(
            "<h2 property=\"erforderlicheUnterlagen\">" +
                "<span>" +
                    "<b>Kfz-Schein Änderung Teil 1</b>" +
                "</span>" +
                "<span property=\"unterlage1\" content=\"<b>Kfz-Schein Änderung Teil 2</b>\">" +
                    "<b>Kfz-Schein Änderung Teil 2</b>" +
                "</span>" +
            "</h2>"
        );
        assertEqualsExpectedWithRefined(bytesHtmlRefinedExpected, bytesHtmlInputRefined);
    }

    @Test
    void ignore_content_attribute_if_tag_has_children_with_property_attributes() {
        String bytesHtmlInput = buildCompleteHtml(
            "<h2 property=\"erforderlicheUnterlagen\" content=\"<b>Namensänderung</b>\">" +
                "<span property=\"unterlage1\">" +
                    "<b>Kfz-Schein Änderung Teil 1</b>" +
                "</span>" +
                "<span property=\"unterlage2\">" +
                    "<b>Kfz-Schein Änderung Teil 2</b>" +
                "</span>" +
            "</h2>"
        );


        byte[] bytesHtmlInputRefined = RDFaRefiner.refine(bytesHtmlInput.getBytes());


        String bytesHtmlRefinedExpected = buildCompleteHtml(
            "<h2 property=\"erforderlicheUnterlagen\" content=\"<b>Namensänderung</b>\">" +
                "<span property=\"unterlage1\" content=\"<b>Kfz-Schein Änderung Teil 1</b>\">" +
                    "<b>Kfz-Schein Änderung Teil 1</b>" +
                "</span>" +
                "<span property=\"unterlage2\" content=\"<b>Kfz-Schein Änderung Teil 2</b>\">" +
                    "<b>Kfz-Schein Änderung Teil 2</b>" +
                "</span>" +
            "</h2>"
        );
        assertEqualsExpectedWithRefined(bytesHtmlRefinedExpected, bytesHtmlInputRefined);
    }


    private void assertEqualsExpectedWithRefined(String bytesHtmlRefinedExpected, byte[] bytesHtmlInputRefined) {
        assertEquals(
            bytesHtmlRefinedExpected.replaceAll("\\r\\n|\\r|\\n| ", ""),
            new String(bytesHtmlInputRefined).replaceAll("\\r\\n|\\r|\\n| ", "")
        );
    }

    private String allowedTagsExample() {
        return (
            "<b>Bold text</b>" +
            "Linebreak<br>" +
            "<p>Paragraph text</p>" +
            "<em>Emphasized text</em>" +
            "<h1>First heading</h1>" +
            "<h2>Second heading</h2>" +
            "<h3>Third heading</h3>" +
            "<h4>Fourth heading</h4>" +
            "<h5>Fifth heading</h5>" +
            "<h6>Sixth heading</h6>" +
            "<i>Italic text</i>" +
            "<strong>Strong text</strong>" +
            "<sub>Subscript text</sub>" +
            "<sup>Superscript text</sup>" +
            "<a href=\"https://www.publicplan.de/\">Publicplan Site with https</a>" +
            "<a href=\"http://www.publicplan.de/\">Publicplan Site with http</a>" +
            "<a href=\"ftp://www.publicplan.de/\">Publicplan Site with ftp</a>" +
            "<a href=\"mailto:info@publicplan.de\">Publicplan Mailto</a>" +
            "<ol>" +
                "<li>First list item in ordered list</li>" +
                "<li>Second list item in ordered list</li>" +
            "</ol>" +
            "<ul>" +
                "<li>First list item in unordered list</li>" +
                "<li>Second list item in unordered list</li>" +
            "</ul>"
        );
    }

    private String buildCompleteHtml(String innerBody) {
        return (
            "<!doctype html>" +
            "<html>" +
                "<head>" +
                    "<meta>" +
                "</head>" +
                "<body>" +
                    innerBody +
                "</body>" +
            "</html>"
        ).replaceAll("\\r\\n|\\r|\\n", "");
    }
}
