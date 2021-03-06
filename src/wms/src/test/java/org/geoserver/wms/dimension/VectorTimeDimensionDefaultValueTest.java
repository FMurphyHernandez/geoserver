/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.Date;
import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;

import javax.xml.namespace.QName;

import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.feature.type.DateUtil;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Tests the WMS default value support for TIME dimension for
 * vector layers.
 * 
 * @author Ilkka Rinne <ilkka.rinne@spatineo.com>
 */
public class VectorTimeDimensionDefaultValueTest extends WMSTestSupport {

    static final QName TIME_WITH_START_END = new QName(MockData.SF_URI, "TimeWithStartEnd",
            MockData.SF_PREFIX);  

    WMS wms;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));        
    }

    @Before
    public void setup() throws Exception {
        wms = getWMS(); //with the initialized application context
        ((SystemTestData)testData).addVectorLayer(TIME_WITH_START_END,Collections.EMPTY_MAP,"TimeElevationWithStartEnd.properties",
                getClass(), getCatalog());
    }
    
    @Test
    public void testDefaultTimeVectorSelector() throws Exception {
        int fid = 1000;
        
        //Use default DimensionInfo setup, should return the "current" time:
        setupFeatureTimeDimension(null);

        FeatureTypeInfo timeWithStartEnd = getCatalog().getFeatureTypeByName(
                TIME_WITH_START_END.getLocalPart());

        Date twoDaysAgo = addFeatureWithTimeTwoDaysAgo(fid++);
        this.addFeature(fid++, twoDaysAgo,Double.valueOf(0d));

        java.util.Date d = wms.getDefaultTime(timeWithStartEnd);
        assertTrue("Default time is null", d != null);
        assertTrue("Default time should be the closest one", d.getTime() == twoDaysAgo.getTime());
        
        // Add some features with timestamps in the future:
        Date dayAfterTomorrow = addFeatureWithTimeDayAfterTomorrow(fid++);
        addFeatureWithTimeOneYearFromNow(fid++);
               
        d = wms.getDefaultTime(timeWithStartEnd);
        assertTrue("Default time is null", d != null);
        assertTrue("Default time should be the closest one", d.getTime() == dayAfterTomorrow.getTime());
        
        Date todayMidnight = addFeatureWithTimeTodayMidnight(fid++);
     
        d = wms.getDefaultTime(timeWithStartEnd);
        assertTrue("Default time is null", d != null);
        assertTrue("Default time should be the closest one", d.getTime() == todayMidnight
                .getTime());
    }
    
    
    @Test
    public void testExplicitCurrentTimeVectorSelector() throws Exception {
        int fid = 1000;
        
        //Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.NEAREST);
        defaultValueSetting.setReferenceValue(DimensionDefaultValueSetting.TIME_CURRENT);
        setupFeatureTimeDimension(defaultValueSetting);

        FeatureTypeInfo timeWithStartEnd = getCatalog().getFeatureTypeByName(
                TIME_WITH_START_END.getLocalPart());

        Date twoDaysAgo = addFeatureWithTimeTwoDaysAgo(fid++);
        this.addFeature(fid++, twoDaysAgo,Double.valueOf(0d));

        java.util.Date d = wms.getDefaultTime(timeWithStartEnd);
        assertTrue("Default time is null", d != null);
        assertTrue("Default time should be the closest one", d.getTime() == twoDaysAgo.getTime());
        
        // Add some features with timestamps in the future:
        Date dayAfterTomorrow = addFeatureWithTimeDayAfterTomorrow(fid++);
        addFeatureWithTimeOneYearFromNow(fid++);
               
        d = wms.getDefaultTime(timeWithStartEnd);
        assertTrue("Default time is null", d != null);
        assertTrue("Default time should be the closest one", d.getTime() == dayAfterTomorrow.getTime());
        
        Date todayMidnight = addFeatureWithTimeTodayMidnight(fid++);
     
        d = wms.getDefaultTime(timeWithStartEnd);
        assertTrue("Default time is null", d != null);
        assertTrue("Default time should be the closest one", d.getTime() == todayMidnight
                .getTime());
    }
    
    @Test
    public void testExplicitMinTimeVectorSelector() throws Exception {
        int fid = 1000;
        
        //Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MINIMUM);

        setupFeatureTimeDimension(defaultValueSetting);

        FeatureTypeInfo timeWithStartEnd = getCatalog().getFeatureTypeByName(
                TIME_WITH_START_END.getLocalPart());
        Date smallest = Date.valueOf("2012-02-11");
        
        Date twoDaysAgo = addFeatureWithTimeTwoDaysAgo(fid++);
        this.addFeature(fid++, twoDaysAgo,Double.valueOf(0d));

        java.util.Date d = wms.getDefaultTime(timeWithStartEnd);
        assertTrue("Default time is null", d != null);
        assertTrue("Default time should be the smallest one", d.getTime() == smallest.getTime());
        
        // Add some features with timestamps in the future:
        addFeatureWithTimeDayAfterTomorrow(fid++);
        addFeatureWithTimeOneYearFromNow(fid++);
               
        d = wms.getDefaultTime(timeWithStartEnd);
        assertTrue("Default time is null", d != null);
        assertTrue("Default time should be the smallest one", d.getTime() == smallest.getTime());
        
        addFeatureWithTimeTodayMidnight(fid++);
     
        d = wms.getDefaultTime(timeWithStartEnd);
        assertTrue("Default time is null", d != null);
        assertTrue("Default time should be the smallest one", d.getTime() == smallest
                .getTime());
    }
    
    @Test
    public void testExplicitMaxTimeVectorSelector() throws Exception {
        int fid = 1000;
        
        //Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MAXIMUM);

        setupFeatureTimeDimension(defaultValueSetting);

        FeatureTypeInfo timeWithStartEnd = getCatalog().getFeatureTypeByName(
                TIME_WITH_START_END.getLocalPart());
        
        Date twoDaysAgo = addFeatureWithTimeTwoDaysAgo(fid++);
        this.addFeature(fid++, twoDaysAgo, Double.valueOf(0d));

        java.util.Date d = wms.getDefaultTime(timeWithStartEnd);
        assertTrue("Default time is null", d != null);
        assertTrue("Default time should be the biggest one", d.getTime() == twoDaysAgo.getTime());
        
        // Add some features with timestamps in the future:
        addFeatureWithTimeDayAfterTomorrow(fid++);
        Date oneYearFromNow = addFeatureWithTimeOneYearFromNow(fid++);
               
        d = wms.getDefaultTime(timeWithStartEnd);
        assertTrue("Default time is null", d != null);
        assertTrue("Default time should be the biggest one", d.getTime() == oneYearFromNow.getTime());
        
        addFeatureWithTimeTodayMidnight(fid++);
     
        d = wms.getDefaultTime(timeWithStartEnd);
        assertTrue("Default time is null", d != null);
        assertTrue("Default time should be the biggest one", d.getTime() == oneYearFromNow
                .getTime());
    }
    
    @Test
    public void testExplicitFixedTimeVectorSelector() throws Exception {
        int fid = 1000;
        String fixedTimeStr = "2012-06-01T03:00:00.000Z";
        
        //Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue(fixedTimeStr);

        long fixedTime = DateUtil.parseDateTime(fixedTimeStr);
        
        setupFeatureTimeDimension(defaultValueSetting);

        FeatureTypeInfo timeWithStartEnd = getCatalog().getFeatureTypeByName(
                TIME_WITH_START_END.getLocalPart());
        
        Date twoDaysAgo = addFeatureWithTimeTwoDaysAgo(fid++);
        this.addFeature(fid++, twoDaysAgo,Double.valueOf(0d));

        java.util.Date d = wms.getDefaultTime(timeWithStartEnd);
        assertTrue("Default time is null", d != null);
        assertTrue("Default time should be the fixed one", d.getTime() == fixedTime);
        
        // Add some features with timestamps in the future:
        addFeatureWithTimeDayAfterTomorrow(fid++);
        addFeatureWithTimeOneYearFromNow(fid++);
               
        d = wms.getDefaultTime(timeWithStartEnd);
        assertTrue("Default time is null", d != null);
        assertTrue("Default time should be the fixed one", d.getTime() == fixedTime);
        
        addFeatureWithTimeTodayMidnight(fid++);
     
        d = wms.getDefaultTime(timeWithStartEnd);
        assertTrue("Default time is null", d != null);
        assertTrue("Default time should be the fixed one", d.getTime() == fixedTime);
    }
    
    @Test
    public void testExplicitNearestToGivenTimeVectorSelector() throws Exception {
        int fid = 1000;
        String preferredTimeStr = "2012-06-01T03:00:00.000Z";
        
        //Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.NEAREST);
        defaultValueSetting.setReferenceValue(preferredTimeStr);
        
        //From src/test/resources/org/geoserver/wms/TimeElevationWithStartEnd.properties:
        Date expected = Date.valueOf("2012-02-12"); 
        
        setupFeatureTimeDimension(defaultValueSetting);

        FeatureTypeInfo timeWithStartEnd = getCatalog().getFeatureTypeByName(
                TIME_WITH_START_END.getLocalPart());
        
        Date twoDaysAgo = addFeatureWithTimeTwoDaysAgo(fid++);
        this.addFeature(fid++, twoDaysAgo, Double.valueOf(0d));

        java.util.Date d = wms.getDefaultTime(timeWithStartEnd);
        assertTrue("Default time is null", d != null);
        assertTrue("Default time should be the closest one", d.getTime() == expected.getTime());
        
        // Add some features with timestamps in the future:
        addFeatureWithTimeDayAfterTomorrow(fid++);
        addFeatureWithTimeOneYearFromNow(fid++);
               
        d = wms.getDefaultTime(timeWithStartEnd);
        assertTrue("Default time is null", d != null);
        assertTrue("Default time should be the closest one", d.getTime() == expected.getTime());
        
        addFeatureWithTimeTodayMidnight(fid++);
     
        d = wms.getDefaultTime(timeWithStartEnd);
        assertTrue("Default time is null", d != null);
        assertTrue("Default time should be the closest one", d.getTime() == expected.getTime());
    }
    
    
   
        
    protected void setupFeatureTimeDimension(DimensionDefaultValueSetting defaultValue) {
        FeatureTypeInfo info = getCatalog()
                .getFeatureTypeByName(TIME_WITH_START_END.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setAttribute("startTime");
        di.setDefaultValue(defaultValue);
        di.setPresentation(DimensionPresentation.LIST);
        info.getMetadata().put(ResourceInfo.TIME, di);
        getCatalog().save(info);
    }     

    protected void addFeature(int id, Date time, Double elevation) throws IOException {
        FeatureTypeInfo timeWithStartEnd = getCatalog().getFeatureTypeByName(
                TIME_WITH_START_END.getLocalPart());
        FeatureStore fs = (FeatureStore) timeWithStartEnd.getFeatureSource(null, null);
        SimpleFeatureType type = (SimpleFeatureType) timeWithStartEnd.getFeatureType();
        MemoryFeatureCollection coll = new MemoryFeatureCollection(type);
        StringBuffer content = new StringBuffer();
        content.append(id);
        content.append('|');
        content.append(time.toString());
        content.append("||");
        content.append(elevation);
        content.append('|');
        
        SimpleFeature f = DataUtilities.createFeature(type, content.toString());
        coll.add(f);
        org.geotools.data.Transaction tx = fs.getTransaction();
        fs.addFeatures(coll);
        tx.commit();
    }

  
    private java.sql.Date addFeatureWithTimeTodayMidnight(int fid) throws IOException{
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
        Date todayMidnight = new Date(cal.getTimeInMillis());
        this.addFeature(fid, todayMidnight,Double.valueOf(0d));
        return todayMidnight;        
    }
    
    private java.sql.Date addFeatureWithTimeTwoDaysAgo(int fid) throws IOException{
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) - 2);
        Date twoDaysAgo = new Date(cal.getTimeInMillis());
        this.addFeature(fid, twoDaysAgo,Double.valueOf(0d));
        return twoDaysAgo;        
    }
    
    private java.sql.Date addFeatureWithTimeDayAfterTomorrow(int fid) throws IOException{
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) + 2);
        
        Date tomorrow = new Date(cal.getTimeInMillis());
        this.addFeature(fid++, tomorrow, Double.valueOf(0d));
        return tomorrow;        
    }
    
    private java.sql.Date addFeatureWithTimeOneYearFromNow(int fid) throws IOException{
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
        
        Date oneYearFromNow = new Date(cal.getTimeInMillis());
        this.addFeature(fid++, oneYearFromNow, Double.valueOf(0d));
        return oneYearFromNow;        
    }
     
}
