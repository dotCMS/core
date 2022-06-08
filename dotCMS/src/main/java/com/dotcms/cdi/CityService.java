package com.dotcms.cdi;



import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class CityService implements ICityService{
    @Inject
    private ICityDao cityDao;

    @Override
    public List<City> getCities() {

        return cityDao.findAll();
    }
}
