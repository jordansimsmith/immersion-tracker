/*
 * This file is generated by jOOQ.
 */
package com.jordansimsmith.immersion.tracker.jooq.tables.records;


import com.jordansimsmith.immersion.tracker.jooq.tables.Show;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record5;
import org.jooq.Row5;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ShowRecord extends UpdatableRecordImpl<ShowRecord> implements Record5<Integer, String, Integer, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.show.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.show.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>public.show.folder_name</code>.
     */
    public void setFolderName(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.show.folder_name</code>.
     */
    public String getFolderName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>public.show.tvdb_id</code>.
     */
    public void setTvdbId(Integer value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.show.tvdb_id</code>.
     */
    public Integer getTvdbId() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>public.show.tvdb_name</code>.
     */
    public void setTvdbName(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>public.show.tvdb_name</code>.
     */
    public String getTvdbName() {
        return (String) get(3);
    }

    /**
     * Setter for <code>public.show.tvdb_image</code>.
     */
    public void setTvdbImage(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>public.show.tvdb_image</code>.
     */
    public String getTvdbImage() {
        return (String) get(4);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record5 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row5<Integer, String, Integer, String, String> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    @Override
    public Row5<Integer, String, Integer, String, String> valuesRow() {
        return (Row5) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Show.SHOW.ID;
    }

    @Override
    public Field<String> field2() {
        return Show.SHOW.FOLDER_NAME;
    }

    @Override
    public Field<Integer> field3() {
        return Show.SHOW.TVDB_ID;
    }

    @Override
    public Field<String> field4() {
        return Show.SHOW.TVDB_NAME;
    }

    @Override
    public Field<String> field5() {
        return Show.SHOW.TVDB_IMAGE;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getFolderName();
    }

    @Override
    public Integer component3() {
        return getTvdbId();
    }

    @Override
    public String component4() {
        return getTvdbName();
    }

    @Override
    public String component5() {
        return getTvdbImage();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getFolderName();
    }

    @Override
    public Integer value3() {
        return getTvdbId();
    }

    @Override
    public String value4() {
        return getTvdbName();
    }

    @Override
    public String value5() {
        return getTvdbImage();
    }

    @Override
    public ShowRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public ShowRecord value2(String value) {
        setFolderName(value);
        return this;
    }

    @Override
    public ShowRecord value3(Integer value) {
        setTvdbId(value);
        return this;
    }

    @Override
    public ShowRecord value4(String value) {
        setTvdbName(value);
        return this;
    }

    @Override
    public ShowRecord value5(String value) {
        setTvdbImage(value);
        return this;
    }

    @Override
    public ShowRecord values(Integer value1, String value2, Integer value3, String value4, String value5) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ShowRecord
     */
    public ShowRecord() {
        super(Show.SHOW);
    }

    /**
     * Create a detached, initialised ShowRecord
     */
    public ShowRecord(Integer id, String folderName, Integer tvdbId, String tvdbName, String tvdbImage) {
        super(Show.SHOW);

        setId(id);
        setFolderName(folderName);
        setTvdbId(tvdbId);
        setTvdbName(tvdbName);
        setTvdbImage(tvdbImage);
        resetChangedOnNotNull();
    }
}
