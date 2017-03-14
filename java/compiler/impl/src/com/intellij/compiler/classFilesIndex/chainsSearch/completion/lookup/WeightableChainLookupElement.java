/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.compiler.classFilesIndex.chainsSearch.completion.lookup;

import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.compiler.classFilesIndex.chainsSearch.ChainRelevance;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementDecorator;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dmitry Batkovich
 */
public final class WeightableChainLookupElement extends LookupElementDecorator<LookupElement> {
  private final ChainRelevance myChainRelevance;

  public WeightableChainLookupElement(final @NotNull LookupElement delegate, final ChainRelevance relevance) {
    super(delegate);
    myChainRelevance = relevance;
  }

  public ChainRelevance getChainRelevance() {
    return myChainRelevance;
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    presentation.setItemTextForeground(JBColor.GREEN);
    super.renderElement(presentation);
  }
}
