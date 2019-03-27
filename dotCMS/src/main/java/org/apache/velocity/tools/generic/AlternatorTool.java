/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.velocity.tools.generic;

import java.util.List;

/**
 * Simple tool to provide easy in-template instantiation of
 * {@link Alternator}s from varying "list" types.
 *
 * <p><b>Example Use:</b>
 * <pre>
 * toolbox.xml...
 * &lt;tool&gt;
 *   &lt;key&gt;alternator&lt;/key&gt;
 *   &lt;scope&gt;application&lt;/scope&gt;
 *   &lt;class&gt;org.apache.velocity.tools.generic.AlternatorTool&lt;/class&gt;
 * &lt;/tool&gt;
 *
 * template...
 * #set( $color = $alternator.make('red', 'blue') )
 * ## use manual alternation for this one
 * #set( $style = $alternator.make(false, ['hip','fly','groovy']) )
 * #foreach( $i in [1..5] )
 *  $i is $color and $style.next
 * #end
 *
 * output...
 *  1 is red and hip
 *  2 is blue and fly 
 *  3 is red and groovy
 *  4 is blue and hip 
 *  5 is red and fly
 * </pre></p>
 *
 * @since Velocity Tools 1.2
 * @version $Revision: 72056 $ $Date: 2004-05-05 17:01:27 -0700 (Wed, 05 May 2004) $
 */
public class AlternatorTool
{

    public AlternatorTool() {}

    /**
     * Make an automatic {@link Alternator} from a List.
     */
    public Alternator make(List list)
    {
        return make(false, list);
    }

    /**
     * Make an {@link Alternator} from a List.
     *
     * @return The new Alternator, or <code>null</code> if arguments
     * were illegal.
     */
    public Alternator make(boolean auto, List list)
    {
        if (list == null)
        {
            return null;
        }
        return new Alternator(auto, list);
    }

    /**
     * Make an automatic {@link Alternator} from an object array.
     */
    public Alternator make(Object[] array)
    {
        return make(false, array);
    }

    /**
     * Make an {@link Alternator} from an object array.
     *
     * @return The new Alternator, or <code>null</code> if arguments
     * were illegal.
     */
    public Alternator make(boolean auto, Object[] array)
    {
        if (array == null)
        {
            return null;
        }
        return new Alternator(auto, array);
    }

    /**
     * Make an automatic {@link Alternator} from a list containing the two
     * specified objects.
     *
     * @return The new Alternator, or <code>null</code> if arguments
     * were illegal.
     */
    public Alternator make(Object o1, Object o2)
    {
        return make(false, o1, o2);
    }

    /**
     * Make an {@link Alternator} from a list containing the two
     * specified objects.
     *
     * @param o1 The first of two objects for alternation between.
     * Must be non-<code>null</code>.
     * @param o2 The second of two objects for alternation between.
     * Must be non-<code>null</code>.
     * @return The new Alternator, or <code>null</code> if arguments
     * were illegal.
     */
    public Alternator make(boolean auto, Object o1, Object o2)
    {
        if (o1 == null || o2 == null)
        {
            return null;
        }
        return new Alternator(auto, new Object[] { o1, o2 });
    }

}
