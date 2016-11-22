package com.dotcms.contenttype.business;

import java.util.List;
import java.util.Set;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;


public interface ContentTypeApi {



  final Set<String> reservedStructureNames = ImmutableSet.of("host", "folder", "file", "forms", "html page",
      "menu link", "virtual link", "container", "template", "user");

  final Set<String> reservedStructureVars = ImmutableSet.of("host", "folder", "file", "forms", "htmlpage", "menulink",
      "virtuallink", "container", "template", "user", "calendarEvent");

  void delete(ContentType st) throws DotSecurityException, DotDataException;

  ContentType find(String inode) throws DotSecurityException, DotDataException;

  List<ContentType> findAll() throws DotDataException;

  ContentType findDefault() throws DotDataException, DotSecurityException;

  List<ContentType> findByBaseType(BaseContentType type, String orderBy, int limit, int offset) throws DotDataException;

  List<ContentType> findByType(BaseContentType type) throws DotDataException, DotSecurityException;

  List<ContentType> findAll(String orderBy) throws DotDataException;

  List<ContentType> findUrlMapped() throws DotDataException;

  // based on a condition
  int count(String condition) throws DotDataException;


  // based on a condition and a user
  int count(String condition, BaseContentType base) throws DotDataException;


  // all
  int count() throws DotDataException;

  String suggestVelocityVar(String tryVar) throws DotDataException;

  ContentType setAsDefault(ContentType type) throws DotDataException, DotSecurityException;


  List<SimpleStructureURLMap> findStructureURLMapPatterns() throws DotDataException;

  void moveToSystemFolder(Folder folder) throws DotDataException;

  ContentType save(ContentType type, List<Field> fields) throws DotDataException, DotSecurityException;

  ContentType save(ContentType type) throws DotDataException, DotSecurityException;


  List<ContentType> recentlyUsed(BaseContentType type, int numberToShow) throws DotDataException;


  List<ContentType> search(String condition) throws DotDataException;


  List<ContentType> search(String condition, String orderBy, int limit, int offset) throws DotDataException;

  List<ContentType> search(String condition, BaseContentType base, String orderBy, int limit, int offset)
      throws DotDataException;



}
