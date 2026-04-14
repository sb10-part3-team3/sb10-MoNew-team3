package com.team3.monew;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GitMessageTemplateTest {

    private static List<String> lines;
    private static Path templatePath;

    @BeforeAll
    static void loadTemplate() throws IOException {
        templatePath = Paths.get(".gitmessage.txt").toAbsolutePath();
        lines = Files.readAllLines(templatePath);
    }

    @Test
    void templateFileShouldExist() {
        assertTrue(Files.exists(templatePath), ".gitmessage.txt should exist at project root");
    }

    @Test
    void templateFileShouldNotBeEmpty() {
        assertFalse(lines.isEmpty(), ".gitmessage.txt should not be empty");
    }

    @Test
    void footerExampleShouldUseFourDigitIssueNumber() {
        boolean found = lines.stream()
                .anyMatch(line -> line.contains("fix: #1234"));
        assertTrue(found, "Footer example should reference a 4-digit issue number (#1234)");
    }

    @Test
    void footerExampleShouldNotUseThreeDigitIssueNumber() {
        // The old example was "fix: #123" — ensure it has been replaced with "#1234"
        boolean oldFormatPresent = lines.stream()
                .anyMatch(line -> line.matches(".*fix: #123(?!\\d).*"));
        assertFalse(oldFormatPresent,
                "Footer example should not use the old 3-digit issue number (#123)");
    }

    @Test
    void footerSectionShouldContainIssueNumberExample() {
        // Find the footer section header and verify the example follows it
        int footerIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("꼬리말")) {
                footerIndex = i;
                break;
            }
        }
        assertNotSame(-1, footerIndex, "Footer section (꼬리말) should be present in the template");

        boolean exampleInFooterSection = false;
        for (int i = footerIndex; i < lines.size(); i++) {
            if (lines.get(i).contains("fix: #1234")) {
                exampleInFooterSection = true;
                break;
            }
            // Stop searching if we hit another section header
            if (i > footerIndex && lines.get(i).contains("---") && !lines.get(i).equals(lines.get(footerIndex))) {
                break;
            }
        }
        assertTrue(exampleInFooterSection,
                "The 4-digit issue number example (fix: #1234) should appear in the footer section");
    }

    @Test
    void allNonBlankLinesShouldBeComments() {
        List<String> nonCommentNonBlankLines = lines.stream()
                .filter(line -> !line.isBlank())
                .filter(line -> !line.startsWith("#"))
                .collect(Collectors.toList());
        assertTrue(nonCommentNonBlankLines.isEmpty(),
                "All non-blank lines in the template should be comments starting with '#', but found: "
                        + nonCommentNonBlankLines);
    }

    @Test
    void templateShouldContainRequiredTypeSections() {
        String content = String.join("\n", lines);
        assertAll("commit type keywords should be present",
                () -> assertTrue(content.contains("feat"), "Template should document 'feat' type"),
                () -> assertTrue(content.contains("fix"), "Template should document 'fix' type"),
                () -> assertTrue(content.contains("docs"), "Template should document 'docs' type"),
                () -> assertTrue(content.contains("refactor"), "Template should document 'refactor' type"),
                () -> assertTrue(content.contains("test"), "Template should document 'test' type"),
                () -> assertTrue(content.contains("chore"), "Template should document 'chore' type")
        );
    }

    @Test
    void issueExampleShouldHaveExactlyFourDigits() {
        // Regression: confirm the example is exactly #1234 (4 digits), not more or fewer
        boolean exactFourDigitExample = lines.stream()
                .anyMatch(line -> line.matches(".*#1234(?!\\d).*"));
        assertTrue(exactFourDigitExample,
                "The issue number in the footer example should be exactly 4 digits (#1234)");
    }
}