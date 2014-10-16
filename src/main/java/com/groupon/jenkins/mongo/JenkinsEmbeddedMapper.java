/*
The MIT License (MIT)

Copyright (c) 2014, Groupon, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package com.groupon.jenkins.mongo;

import com.groupon.jenkins.dynamic.build.DbBackedProject;
import com.groupon.jenkins.dynamic.build.DynamicProject;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import hudson.matrix.AxisList;
import hudson.util.CopyOnWriteList;
import org.apache.tools.ant.taskdefs.Copy;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.CustomMapper;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.cache.EntityCache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class JenkinsEmbeddedMapper implements CustomMapper {
    public static final Set<Class> PROBLEMATIC_CLASSES = new HashSet<Class>(Arrays.asList(AxisList.class, DynamicProject.class, DbBackedProject.class));
    private final Map<Class, CustomMapper> customMappers;
    JenkinsEmbeddedMapper() {
        customMappers = new HashMap<Class, CustomMapper>();
        customMappers.put(CopyOnWriteList.class, new CopyOnWriteListMapper());
    }

    @Override
    public void toDBObject(Object entity, MappedField mf, DBObject dbObject, Map<Object, DBObject> involvedObjects, Mapper mapper) {
        Object fieldValue = mf.getFieldValue(entity);
Object idvalue = mapper.getId(fieldValue);
        if(customMappers.containsKey(entity.getClass())) {
            customMappers.get(entity.getClass()).toDBObject(entity, mf, dbObject, involvedObjects, mapper);
        } else if(fieldValue != null
                && mapper.getId(fieldValue) != null
                && involvedObjects.containsKey(fieldValue)
                && (involvedObjects.get(fieldValue) == null ||involvedObjects.get(fieldValue).keySet().size() == 2)) {

            Object id = mapper.getId(fieldValue);
            if(id != null) {
                DBObject refObj = new BasicDBObject(mapper.ID_KEY, id);

                refObj.put(mapper.CLASS_NAME_FIELDNAME, fieldValue.getClass().getName());

                involvedObjects.put(entity, refObj);

                dbObject.put(mf.getNameToStore(), refObj);
            }
        } else {
            mapper.getOptions().getEmbeddedMapper().toDBObject(entity, mf, dbObject, involvedObjects, mapper);
        }
    }

    private Key extractKey(Mapper mapper, MappedField mf, DBObject dbObject) {
        if(mapper == null || mf == null || dbObject == null ||  (dbObject instanceof BasicDBList)) return null;

        ObjectId objectId = (ObjectId) dbObject.get("_id");
        //HACKY GET RID OF SOON
        Object obj = mapper.getOptions().getObjectFactory().createInstance(mapper, mf, dbObject);

        if(objectId == null || obj == null) return null;

        return new Key(obj.getClass(), objectId);
    }
    @Override
    public void fromDBObject(DBObject dbObject, MappedField mf, Object entity, EntityCache cache, Mapper mapper) {

        Key key = extractKey(mapper, mf, (DBObject) dbObject.get(mf.getNameToStore()));
        if(key != null && cache.exists(key)) {
            Object object = cache.getEntity(key);
            mf.setFieldValue(entity, object);
        } else if(customMappers.containsKey(mf.getType())) {
            customMappers.get(mf.getType()).fromDBObject(dbObject, mf, entity, cache, mapper);
        } else {
            mapper.getOptions().getEmbeddedMapper().fromDBObject(dbObject, mf, entity, cache, mapper);
        }
    }


}

class CopyOnWriteListMapper implements CustomMapper {
    @Override
    public void toDBObject(Object entity, MappedField mf, DBObject dbObject, Map<Object, DBObject> involvedObjects, Mapper mapper) {
        final String name = mf.getNameToStore();
        CopyOnWriteList copyOnWriteList = (CopyOnWriteList) entity;
        List core = new ArrayList();

        for(Object obj : copyOnWriteList) {
            core.add(mapper.toDBObject(obj, involvedObjects));
        }

        dbObject.put(name, core);
    }

    @Override
    public void fromDBObject(DBObject dbObject, MappedField mf, Object entity, EntityCache cache, Mapper mapper) {
        DBObject cowlObject = (DBObject) dbObject.get(mf.getNameToStore());
        BasicDBList rawList = (BasicDBList) cowlObject.get("core");
        List core = new ArrayList();
        for(Object obj : rawList) {
            DBObject listEntryDbObj = (DBObject) obj;

            // Hack until we can coax MappedField to understand what CopyOnWriteList is. Eliminate as soon as possible.
            // Currently mf.getSubType() is null because MappedField does not use Iterable to determine a list and thus
            // does not check for subtypes.
            Class clazz = mapper.getOptions().getObjectFactory().createInstance(mapper, mf, listEntryDbObj).getClass();

            core.add(mapper.fromDBObject(clazz, listEntryDbObj, cache));
        }
        mf.setFieldValue(entity, new CopyOnWriteList(core));
    }
}
