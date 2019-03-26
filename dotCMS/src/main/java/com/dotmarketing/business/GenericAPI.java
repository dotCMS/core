package com.dotmarketing.business;

import com.dotmarketing.exception.DotDataException;
import java.util.List;

public interface GenericAPI<T> {

  public T find(String id) throws DotDataException;

  public void delete(T object) throws DotDataException;

  public List<T> findAll() throws DotDataException;

  public void save(T object) throws DotDataException;
}
