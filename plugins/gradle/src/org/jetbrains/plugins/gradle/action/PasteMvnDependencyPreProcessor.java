/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.gradle.action;

import com.intellij.codeInsight.editorActions.CopyPastePreProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RawText;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.statistics.GradleActionsUsagesCollector;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.gradle.util.GradleUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;

public class PasteMvnDependencyPreProcessor implements CopyPastePreProcessor {

  @Nullable
  @Override
  public String preprocessOnCopy(PsiFile file, int[] startOffsets, int[] endOffsets, String text) {
    return null;
  }

  @NotNull
  @Override
  public String preprocessOnPaste(Project project, PsiFile file, Editor editor, String text, RawText rawText) {
    if (isApplicable(file) && isMvnDependency(text)) {
      GradleActionsUsagesCollector.trigger(project, GradleActionsUsagesCollector.ActionID.PasteMvnDependency);
      GradleVersion gradleVersion = GradleUtil.getGradleVersion(project, file);
      return toGradleDependency(text, gradleVersion);
    }
    return text;
  }

  protected boolean isApplicable(PsiFile file) {
    return file.getName().endsWith('.' + GradleConstants.EXTENSION);
  }

  @NotNull
  protected String formatGradleDependency(@NotNull String groupId,
                                          @NotNull String artifactId,
                                          @NotNull String version,
                                          @NotNull String scope,
                                          @NotNull String classifier) {
    String gradleClassifier = classifier.isEmpty() ? "" : ":" + classifier;
    return scope + " '" + groupId + ":" + artifactId + ":" + version + gradleClassifier + "'";
  }

  private String toGradleDependency(final String mavenDependency, @Nullable GradleVersion gradleVersion) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(false);

    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      try {
        Document document = builder.parse(new InputSource(new StringReader(mavenDependency)));
        String gradleDependency = extractGradleDependency(document, gradleVersion);
        return gradleDependency != null ? gradleDependency : mavenDependency;
      }
      catch (SAXException | IOException e) {
      }
    }
    catch (ParserConfigurationException e) {
    }

    return mavenDependency;
  }

  @Nullable
  private String extractGradleDependency(Document document, @Nullable GradleVersion gradleVersion) {
    String groupId = getGroupId(document);
    String artifactId = getArtifactId(document);
    String version = getVersion(document);
    String scope = getScope(document, gradleVersion);
    String classifier = getClassifier(document);

    if (groupId.isEmpty() || artifactId.isEmpty() || version.isEmpty()) {
      return null;
    }
    return formatGradleDependency(groupId, artifactId, version, scope, classifier);
  }

  @NotNull
  private static String getScope(@NotNull Document document, @Nullable GradleVersion gradleVersion) {
    String scope = firstOrEmpty(document.getElementsByTagName("scope"));
    boolean isSupportedImplementation = GradleUtil.isSupportedImplementationScope(gradleVersion);
    switch (scope) {
      case "test":
        return isSupportedImplementation ? "testImplementation" : "testCompile";
      case "provided":
        return "compileOnly";
      case "runtime":
        return "runtime";
      case "compile":
      default:
        return isSupportedImplementation ? "implementation" : "compile";
    }
  }

  private static String getVersion(@NotNull Document document) {
    return firstOrEmpty(document.getElementsByTagName("version"));
  }

  private static String getArtifactId(@NotNull Document document) {
    return firstOrEmpty(document.getElementsByTagName("artifactId"));
  }

  private static String getGroupId(@NotNull Document document) {
    return firstOrEmpty(document.getElementsByTagName("groupId"));
  }

  private static String getClassifier(@NotNull Document document) {
    return firstOrEmpty(document.getElementsByTagName("classifier"));
  }

  private static String firstOrEmpty(@NotNull NodeList list) {
    Node first = list.item(0);
    return first != null ? first.getTextContent() : "";
  }

  private static boolean isMvnDependency(String text) {
    String trimmed = trimLeadingComment(text.trim());
    if (trimmed.startsWith("<dependency>") && trimmed.endsWith("</dependency>")) {
      return true;
    }
    return false;
  }

  /**
   * Removes leading comment, usually it exists if dependency was copied from maven central site
   */
  private static String trimLeadingComment(String text) {
    int start = text.indexOf("<!--");
    int end = text.indexOf("-->");
    if (start == 0 && end > 0) {
      return text.substring(end + "-->".length()).trim();
    }
    else {
      return text;
    }
  }

  @Override
  public boolean requiresAllDocumentsToBeCommitted(@NotNull Editor editor, @NotNull Project project) {
    return false;
  }
}