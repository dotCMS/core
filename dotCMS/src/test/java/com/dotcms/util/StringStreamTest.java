package com.dotcms.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class StringStreamTest {

  @Test
  public void lowercaseTest() {

    final List<String> list = Arrays.asList("Hello", "Hello", "Hi", "yEAH", "hI", "hEllO");

    final List<String> lowerList =
        StringStream.of(list.stream()).lowerCase().stream().collect(Collectors.toList());

    assertNotNull(lowerList);
    assertEquals(list.size(), lowerList.size());
    assertEquals(Arrays.asList("hello", "hello", "hi", "yeah", "hi", "hello"), lowerList);
  }

  @Test
  public void uppercaseTest() {

    final List<String> list = Arrays.asList("Hello", "Hello", "Hi", "yEAH", "hI", "hEllO");

    final List<String> lowerList =
        StringStream.of(list.stream()).upperCase().stream().collect(Collectors.toList());

    assertNotNull(lowerList);
    assertEquals(list.size(), lowerList.size());
    assertEquals(Arrays.asList("HELLO", "HELLO", "HI", "YEAH", "HI", "HELLO"), lowerList);
  }
}
