package net.yacy.document.parser.rdfa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RDFaRefinerTest {

    @Test
    void refine() {
        byte[] bytesHtmlInput = (
            "<!doctype html>" +
            "<html>" +
                "<head>" +
                    "<meta>" +
                "</head>" +
                "<body>" +
                    "<p>Hello World</p>" +
                    "<div property=\"blub\">" +
                        "<p property=\"blubInner\">" +
                            "<span property=\"blubInnerInner\">" +
                                "<b>Blub</b>" +
                                "<script></script>" +
                            "</span>" +
                            "<span property=\"blubInnerInner2\" content=\"<i>alreadyContent</i>\">" +
                                "<b>Blub</b>" +
                            "</span>" +
                        "</p>" +
                    "</div>" +
                "</body>" +
            "</html>"
        ).replaceAll("\\r\\n|\\r|\\n", "").getBytes();


        byte[] bytesHtmlInputRefined = RDFaRefiner.refine(bytesHtmlInput);


        String bytesHtmlRefinedExpected = (
            "<!doctype html>" +
                "<html>" +
                    "<head>" +
                        "<meta>" +
                    "</head>" +
                    "<body>" +
                        "<p>Hello World</p>" +
                        "<div property=\"blub\">" +
                            "<p property=\"blubInner\">" +
                                "<span property=\"blubInnerInner\" content=\"<b>Blub</b>\">" +
                                    "<b>Blub</b>" +
                                    "<script></script>" +
                                "</span>" +
                                "<span property=\"blubInnerInner2\" content=\"<i>alreadyContent</i>\">" +
                                    "<b>Blub</b>" +
                                "</span>" +
                            "</p>" +
                        "</div>" +
                    "</body>" +
                "</html>"
        );
        assertEquals(
            bytesHtmlRefinedExpected.replaceAll("\\r\\n|\\r|\\n| ", ""),
            new String(bytesHtmlInputRefined).replaceAll("\\r\\n|\\r|\\n| ", "")
        );
    }
}
