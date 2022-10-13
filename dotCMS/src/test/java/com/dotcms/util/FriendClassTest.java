package com.dotcms.util;

import com.dotcms.UnitTestBase;
import org.junit.Assert;
import org.junit.Test;

public class FriendClassTest extends UnitTestBase {

    @Test
    public void isFriendTest()  {

        new VeryGoodFriendClass().testVeryGoodFriendClass();
        new GoodFriendClass().testGoodFriendClass();
        new NotFriendClass().testNoFriendClass();
    }

    public static class HostClass {

        private final FriendClass friendClass =
                new FriendClass(VeryGoodFriendClass.class, GoodFriendClass.class);

        public boolean test () {

            return this.friendClass.isFriend();
        }
    }

    public static class VeryGoodFriendClass {

        private final HostClass hostClass = new HostClass();
        public void testVeryGoodFriendClass () {

            Assert.assertTrue(this.hostClass.test());
        }
    }

    public static class GoodFriendClass {

        private final HostClass hostClass = new HostClass();
        public void testGoodFriendClass () {

            Assert.assertTrue(this.hostClass.test());
        }
    }

    public static class NotFriendClass {

        private final HostClass hostClass = new HostClass();
        public void testNoFriendClass () {

            Assert.assertFalse(this.hostClass.test());
        }
    }
}
