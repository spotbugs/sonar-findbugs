package org.sonar.plugins.findbugs.language;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.plugins.findbugs.language.lex.PageLexer;
import org.sonar.plugins.findbugs.language.visitor.HtmlAstScanner;
import org.sonar.plugins.findbugs.language.visitor.NoSonarScanner;
import org.sonar.plugins.findbugs.language.visitor.WebSourceCode;

import java.io.FileReader;

public class JspSyntaxSensor implements Sensor {

    private static final Logger LOG = LoggerFactory.getLogger(JspSyntaxSensor.class);

    private final NoSonarFilter noSonarFilter;
    private final FileLinesContextFactory fileLinesContextFactory;

    public JspSyntaxSensor(NoSonarFilter noSonarFilter, FileLinesContextFactory fileLinesContextFactory, CheckFactory checkFactory) {
        this.noSonarFilter = noSonarFilter;
        this.fileLinesContextFactory = fileLinesContextFactory;
    }

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor
                .name(Jsp.KEY)
                .onlyOnFileType(InputFile.Type.MAIN)
                .onlyOnLanguage(Jsp.KEY);
    }

    @Override
    public void execute(SensorContext sensorContext) {
        final PageLexer lexer = new PageLexer();

        FileSystem fileSystem = sensorContext.fileSystem();

        final HtmlAstScanner scanner = setupScanner(sensorContext);

        FilePredicates predicates = fileSystem.predicates();
        Iterable<InputFile> inputFiles = fileSystem.inputFiles(
                predicates.and(
                        predicates.hasType(InputFile.Type.MAIN),
                        predicates.hasLanguage(Jsp.KEY))
        );

        for (InputFile inputFile : inputFiles) {
            WebSourceCode sourceCode = new WebSourceCode(inputFile);

            try (FileReader reader = new FileReader(inputFile.file())) {
                scanner.scan(lexer.parse(reader), sourceCode, fileSystem.encoding());

            } catch (Exception e) {
                LOG.error("Cannot analyze file " + inputFile.file().getAbsolutePath(), e);
            }
        }
    }

    /**
     * Create PageScanner with Visitors.
     */
    private HtmlAstScanner setupScanner(SensorContext context) {
        HtmlAstScanner scanner = new HtmlAstScanner(ImmutableList.of(
                new JspTokensVisitor(context),
                new NoSonarScanner(noSonarFilter)));

        return scanner;
    }
}
