/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Trinity;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;

import java.util.*;

public class FrameMap {
    private final TObjectIntHashMap<DeclarationDescriptor> myVarIndex = new TObjectIntHashMap<DeclarationDescriptor>();
    private final TObjectIntHashMap<DeclarationDescriptor> myVarSizes = new TObjectIntHashMap<DeclarationDescriptor>();
    private int myMaxIndex = 0;

    private Stack<TypeAndDescriptor> currentVars = new Stack<TypeAndDescriptor>();

    public int enter(DeclarationDescriptor descriptor, Type type) {
        int index = myMaxIndex;
        myVarIndex.put(descriptor, index);
        myMaxIndex += type.getSize();
        myVarSizes.put(descriptor, type.getSize());
        currentVars.push(new TypeAndDescriptor(descriptor, type));
        return index;
    }

    public int leave(DeclarationDescriptor descriptor) {
        int size = myVarSizes.get(descriptor);
        myMaxIndex -= size;
        myVarSizes.remove(descriptor);
        int oldIndex = myVarIndex.remove(descriptor);
        if (oldIndex != myMaxIndex) {
            throw new IllegalStateException("Descriptor can be left only if it is last: " + descriptor);
        }
        currentVars.pop();
        return oldIndex;

    }

    public int enterTemp(Type type) {
        int result = myMaxIndex;
        myMaxIndex += type.getSize();
        currentVars.push(new TypeAndDescriptor(null, type));
        return result;
    }

    public void leaveTemp(Type type) {
        myMaxIndex -= type.getSize();
        currentVars.pop();
    }

    public Stack<TypeAndDescriptor> getCurrentVars() {
        return currentVars;
    }

    public int getIndex(DeclarationDescriptor descriptor) {
        return myVarIndex.contains(descriptor) ? myVarIndex.get(descriptor) : -1;
    }

    public Mark mark() {
        return new Mark(myMaxIndex);
    }

    public int getCurrentSize() {
        return myMaxIndex;
    }

    public class Mark {
        private final int myIndex;

        public Mark(int index) {
            myIndex = index;
        }

        public void dropTo() {
            List<DeclarationDescriptor> descriptorsToDrop = new ArrayList<DeclarationDescriptor>();
            TObjectIntIterator<DeclarationDescriptor> iterator = myVarIndex.iterator();
            while (iterator.hasNext()) {
                iterator.advance();
                if (iterator.value() >= myIndex) {
                    descriptorsToDrop.add(iterator.key());
                }
            }
            for (DeclarationDescriptor declarationDescriptor : descriptorsToDrop) {
                myVarIndex.remove(declarationDescriptor);
                myVarSizes.remove(declarationDescriptor);
            }
            myMaxIndex = myIndex;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (myVarIndex.size() != myVarSizes.size()) {
            return "inconsistent";
        }

        List<Trinity<DeclarationDescriptor, Integer, Integer>> descriptors = Lists.newArrayList();

        for (Object descriptor0 : myVarIndex.keys()) {
            DeclarationDescriptor descriptor = (DeclarationDescriptor) descriptor0;
            int varIndex = myVarIndex.get(descriptor);
            int varSize = myVarSizes.get(descriptor);
            descriptors.add(Trinity.create(descriptor, varIndex, varSize));
        }

        Collections.sort(descriptors, new Comparator<Trinity<DeclarationDescriptor, Integer, Integer>>() {
            @Override
            public int compare(
                    Trinity<DeclarationDescriptor, Integer, Integer> left,
                    Trinity<DeclarationDescriptor, Integer, Integer> right
            ) {
                return left.second - right.second;
            }
        });

        sb.append("size=").append(myMaxIndex);

        boolean first = true;
        for (Trinity<DeclarationDescriptor, Integer, Integer> t : descriptors) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(t.first).append(",i=").append(t.second).append(",s=").append(t.third);
        }

        return sb.toString();
    }
}
