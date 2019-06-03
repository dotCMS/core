package com.dotmarketing.image.filter;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ResizeImageFilterTest {

 
  @Test
  public void test_resize_image_filter() throws Exception {
    ResizeImageFilter filter = new ResizeImageFilter();

    File incoming = new File(this.getClass().getClassLoader().getResource("images/SqcP9KgFqruagXJfe7CES.png").getFile());

    Map<String, String[]> map = new HashMap<>();
    map.put(filter.getPrefix() + "w", new String[] {"100"});
    filter.runFilter(incoming,map);

  }

}
