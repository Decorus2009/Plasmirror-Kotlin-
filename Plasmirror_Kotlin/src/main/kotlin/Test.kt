package org.fxmisc.richtext.demo

import java.util.Collections
import java.util.regex.Matcher
import java.util.regex.Pattern

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder

class JavaKeywords : Application() {

    override fun start(primaryStage: Stage) {
        val codeArea = CodeArea()
        codeArea.paragraphGraphicFactory = LineNumberFactory.get(codeArea)

        codeArea.richChanges()
                .filter { ch -> ch.inserted != ch.removed } // XXX
                .subscribe { change -> codeArea.setStyleSpans(0, computeHighlighting(codeArea.text)) }
        codeArea.replaceText(0, 0, sampleCode)

        val scene = Scene(StackPane(VirtualizedScrollPane(codeArea)), 600.0, 400.0)
            scene.stylesheets.add("java-keywords.css")
        primaryStage.scene = scene
        primaryStage.title = "Java Keywords Demo"
        primaryStage.show()
    }

    companion object {
        private val COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"
        private val NAME_PATTERN = "\\w+\\s*=\\s*+"
        private val REPEAT_PATTERN = "\\s*[xX]\\s*[0-9]+\\s*"
        private val PATTERN = Pattern.compile("(?<COMMENT>$COMMENT_PATTERN)|(?<NAME>$NAME_PATTERN)|(?<REPEAT>$REPEAT_PATTERN)")
        private val sampleCode =
                """x1
type = 2-2, d = 1000, k = 0.005, x = 0.31


/*
x24
2-1, 12.15, 0.005, 0.0
//8, 10, 0.31, 7.38, 0.18, 5
*/
"""

        @JvmStatic fun main(args: Array<String>) = Application.launch(JavaKeywords::class.java)


        private fun computeHighlighting(text: String): StyleSpans<Collection<String>> {
            val matcher = PATTERN.matcher(text)
            var lastKwEnd = 0
            val spansBuilder = StyleSpansBuilder<Collection<String>>()
            while (matcher.find()) {
                val styleClass = (when {
                    matcher.group("COMMENT") != null -> "comment"
                    matcher.group("NAME") != null -> "name"
                    matcher.group("REPEAT") != null -> "repeat"
                    else -> null
                })!! /* never happens */
                spansBuilder.add(emptyList<String>(), matcher.start() - lastKwEnd)
                spansBuilder.add(setOf(styleClass), matcher.end() - matcher.start())
                lastKwEnd = matcher.end()
            }
            spansBuilder.add(emptyList<String>(), text.length - lastKwEnd)
            return spansBuilder.create()
        }
    }
}