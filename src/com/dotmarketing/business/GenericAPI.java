package com.dotmarketing.business;

import java.util.List;

import com.dotmarketing.exception.DotDataException;

public interface GenericAPI<T> {

	public T find(String id) throws DotDataException;
	public void delete(T object) throws DotDataException;
	public List<T> findAll() throws DotDataException;
	public void save(T object) throws DotDataException;
}
