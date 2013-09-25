package com.dotmarketing.portlets.contentlet.business;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;

/** @author Hibernate CodeGenerator */
public class Contentlet extends WebAsset implements Serializable {

    private static final long serialVersionUID = 1L;

    private FieldAPI fAPI = APILocator.getFieldAPI();

    /** identifier field */
    private String structureInode;

    private long languageId;

    private String[] categories;

    /** Content reviewing fields */
    private String reviewInterval;
    private Date lastReview;
    private Date nextReview;
    private String disabledWysiwyg;

    /** Generic fields */
    // Generic date fields
    private Date date1;
    private Date date2;
    private Date date3;
    private Date date4;
    private Date date5;
    private Date date6;
    private Date date7;
    private Date date8;
    private Date date9;
    private Date date10;
    private Date date11;
    private Date date12;
    private Date date13;
    private Date date14;
    private Date date15;
    private Date date16;
    private Date date17;
    private Date date18;
    private Date date19;
    private Date date20;
    private Date date21;
    private Date date22;
    private Date date23;
    private Date date24;
    private Date date25;

    // Generic text fields
    private String text1;
    private String text2;
    private String text3;
    private String text4;
    private String text5;
    private String text6;
    private String text7;
    private String text8;
    private String text9;
    private String text10;
    private String text11;
    private String text12;
    private String text13;
    private String text14;
    private String text15;
    private String text16;
    private String text17;
    private String text18;
    private String text19;
    private String text20;
    private String text21;
    private String text22;
    private String text23;
    private String text24;
    private String text25;

    // Generic text_area fields
    private String text_area1;
    private String text_area2;
    private String text_area3;
    private String text_area4;
    private String text_area5;
    private String text_area6;
    private String text_area7;
    private String text_area8;
    private String text_area9;
    private String text_area10;
    private String text_area11;
    private String text_area12;
    private String text_area13;
    private String text_area14;
    private String text_area15;
    private String text_area16;
    private String text_area17;
    private String text_area18;
    private String text_area19;
    private String text_area20;
    private String text_area21;
    private String text_area22;
    private String text_area23;
    private String text_area24;
    private String text_area25;

    // Generic long fields
    private long integer1;
    private long integer2;
    private long integer3;
    private long integer4;
    private long integer5;
    private long integer6;
    private long integer7;
    private long integer8;
    private long integer9;
    private long integer10;
    private long integer11;
    private long integer12;
    private long integer13;
    private long integer14;
    private long integer15;
    private long integer16;
    private long integer17;
    private long integer18;
    private long integer19;
    private long integer20;
    private long integer21;
    private long integer22;
    private long integer23;
    private long integer24;
    private long integer25;

    // Generic float fields
    private float float1;
    private float float2;
    private float float3;
    private float float4;
    private float float5;
    private float float6;
    private float float7;
    private float float8;
    private float float9;
    private float float10;
    private float float11;
    private float float12;
    private float float13;
    private float float14;
    private float float15;
    private float float16;
    private float float17;
    private float float18;
    private float float19;
    private float float20;
    private float float21;
    private float float22;
    private float float23;
    private float float24;
    private float float25;

    // Generic float fields
    private boolean bool1;
    private boolean bool2;
    private boolean bool3;
    private boolean bool4;
    private boolean bool5;
    private boolean bool6;
    private boolean bool7;
    private boolean bool8;
    private boolean bool9;
    private boolean bool10;
    private boolean bool11;
    private boolean bool12;
    private boolean bool13;
    private boolean bool14;
    private boolean bool15;
    private boolean bool16;
    private boolean bool17;
    private boolean bool18;
    private boolean bool19;
    private boolean bool20;
    private boolean bool21;
    private boolean bool22;
    private boolean bool23;
    private boolean bool24;
    private boolean bool25;

    //private String folder;

    private File binary1;
    private File binary2;
    private File binary3;
    private File binary4;
    private File binary5;
    private File binary6;
    private File binary7;
    private File binary8;
    private File binary9;
    private File binary10;
    private File binary11;
    private File binary12;
    private File binary13;
    private File binary14;
    private File binary15;
    private File binary16;
    private File binary17;
    private File binary18;
    private File binary19;
    private File binary20;
    private File binary21;
    private File binary22;
    private File binary23;
    private File binary24;
    private File binary25;



    public String getURI(Folder folder) {
        return folder.getInode() +":"+ this.getInode();
    }

    /** default constructor */
    public Contentlet() {
        super.setType("contentlet");
    }

    public String getInode() {
    	if(InodeUtils.isSet(this.inode))
    		return this.inode;

    	return "";
    }

    public void setInode(String inode) {
        this.inode = inode;
    }

    public long getLanguageId() {
        return languageId;
    }

    public void setLanguageId(long languageId) {
        this.languageId = languageId;
    }

    public String getStructureInode() {
        return structureInode;
    }

    public void setStructureInode(String structureInode) {
        this.structureInode = structureInode;
    }
    /**
     */
    public Structure getStructure() {
        Structure structure = StructureCache.getStructureByInode(structureInode);
        return structure;
    }

    public Date getLastReview() {
        return lastReview;
    }

    public void setLastReview(Date lastReview) {
        this.lastReview = lastReview;
    }

    public Date getNextReview() {
        return nextReview;
    }

    public void setNextReview(Date nextReview) {
        this.nextReview = nextReview;
    }

    public String getReviewInterval() {
        return reviewInterval;
    }

    public void setReviewInterval(String reviewInterval) {
        this.reviewInterval = reviewInterval;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public boolean equals(Object other) {
        if (!(other instanceof Contentlet))
            return false;
        Contentlet castOther = (Contentlet) other;
        return new EqualsBuilder().append(this.inode, castOther.inode).isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder().append(inode).toHashCode();
    }

    // Every Web Asset should implement this method!!!
    @SuppressWarnings("unchecked")
	public void copy(Contentlet currentContentlet) {
        // Keep the current inode
        String tempInode = this.inode;

        // This method copy the whole fields
        try {
            //BeanUtils.copyProperties(this, currentContentlet);

        	java.util.Map properties = BeanUtils.describe(currentContentlet);
        	properties.remove("inode");
        	properties.remove("identifier");

        	java.util.Iterator iter = properties.keySet().iterator();
        	String key;
        	for (; iter.hasNext();) {
        		key = (String) iter.next();

        		try {
        			BeanUtils.setProperty(this, key, properties.get(key));
        		} catch (IllegalArgumentException e) {
        		}
        	}


        } catch (IllegalAccessException iae) {
            Logger.error(this, iae.toString());
        } catch (InvocationTargetException ite) {
            Logger.error(this, ite.toString());
        } catch (NoSuchMethodException e) {
        	Logger.error(this, e.toString());
		}
        // this.setStructure_inode(currentContentlet.getStructure_inode());
        Logger.debug(this, "Calling Contentlet Copy Method");
        super.copy(currentContentlet);

        this.inode = tempInode;
    }

    public int compareTo(Contentlet compObject) {
        Contentlet contentlet = (Contentlet) compObject;
        return (contentlet.getTitle().compareTo(this.getTitle()));
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    // ### Generic getter and Setter methods ###
    public boolean isBool1() {
        return bool1;
    }

    public void setBool1(boolean bool1) {
        this.bool1 = bool1;
    }

    public boolean isBool10() {
        return bool10;
    }

    public void setBool10(boolean bool10) {
        this.bool10 = bool10;
    }

    public boolean isBool11() {
        return bool11;
    }

    public void setBool11(boolean bool11) {
        this.bool11 = bool11;
    }

    public boolean isBool12() {
        return bool12;
    }

    public void setBool12(boolean bool12) {
        this.bool12 = bool12;
    }

    public boolean isBool13() {
        return bool13;
    }

    public void setBool13(boolean bool13) {
        this.bool13 = bool13;
    }

    public boolean isBool14() {
        return bool14;
    }

    public void setBool14(boolean bool14) {
        this.bool14 = bool14;
    }

    public boolean isBool15() {
        return bool15;
    }

    public void setBool15(boolean bool15) {
        this.bool15 = bool15;
    }

    public boolean isBool16() {
        return bool16;
    }

    public void setBool16(boolean bool16) {
        this.bool16 = bool16;
    }

    public boolean isBool17() {
        return bool17;
    }

    public void setBool17(boolean bool17) {
        this.bool17 = bool17;
    }

    public boolean isBool18() {
        return bool18;
    }

    public void setBool18(boolean bool18) {
        this.bool18 = bool18;
    }

    public boolean isBool19() {
        return bool19;
    }

    public void setBool19(boolean bool19) {
        this.bool19 = bool19;
    }

    public boolean isBool2() {
        return bool2;
    }

    public void setBool2(boolean bool2) {
        this.bool2 = bool2;
    }

    public boolean isBool20() {
        return bool20;
    }

    public void setBool20(boolean bool20) {
        this.bool20 = bool20;
    }

    public boolean isBool21() {
        return bool21;
    }

    public void setBool21(boolean bool21) {
        this.bool21 = bool21;
    }

    public boolean isBool22() {
        return bool22;
    }

    public void setBool22(boolean bool22) {
        this.bool22 = bool22;
    }

    public boolean isBool23() {
        return bool23;
    }

    public void setBool23(boolean bool23) {
        this.bool23 = bool23;
    }

    public boolean isBool24() {
        return bool24;
    }

    public void setBool24(boolean bool24) {
        this.bool24 = bool24;
    }

    public boolean isBool25() {
        return bool25;
    }

    public void setBool25(boolean bool25) {
        this.bool25 = bool25;
    }

    public boolean isBool3() {
        return bool3;
    }

    public void setBool3(boolean bool3) {
        this.bool3 = bool3;
    }

    public boolean isBool4() {
        return bool4;
    }

    public void setBool4(boolean bool4) {
        this.bool4 = bool4;
    }

    public boolean isBool5() {
        return bool5;
    }

    public void setBool5(boolean bool5) {
        this.bool5 = bool5;
    }

    public boolean isBool6() {
        return bool6;
    }

    public void setBool6(boolean bool6) {
        this.bool6 = bool6;
    }

    public boolean isBool7() {
        return bool7;
    }

    public void setBool7(boolean bool7) {
        this.bool7 = bool7;
    }

    public boolean isBool8() {
        return bool8;
    }

    public void setBool8(boolean bool8) {
        this.bool8 = bool8;
    }

    public boolean isBool9() {
        return bool9;
    }

    public void setBool9(boolean bool9) {
        this.bool9 = bool9;
    }

    public float getFloat1() {
        return float1;
    }

    public void setFloat1(float float1) {
        this.float1 = float1;
    }

    public float getFloat10() {
        return float10;
    }

    public void setFloat10(float float10) {
        this.float10 = float10;
    }

    public float getFloat11() {
        return float11;
    }

    public void setFloat11(float float11) {
        this.float11 = float11;
    }

    public float getFloat12() {
        return float12;
    }

    public void setFloat12(float float12) {
        this.float12 = float12;
    }

    public float getFloat13() {
        return float13;
    }

    public void setFloat13(float float13) {
        this.float13 = float13;
    }

    public float getFloat14() {
        return float14;
    }

    public void setFloat14(float float14) {
        this.float14 = float14;
    }

    public float getFloat15() {
        return float15;
    }

    public void setFloat15(float float15) {
        this.float15 = float15;
    }

    public float getFloat16() {
        return float16;
    }

    public void setFloat16(float float16) {
        this.float16 = float16;
    }

    public float getFloat17() {
        return float17;
    }

    public void setFloat17(float float17) {
        this.float17 = float17;
    }

    public float getFloat18() {
        return float18;
    }

    public void setFloat18(float float18) {
        this.float18 = float18;
    }

    public float getFloat19() {
        return float19;
    }

    public void setFloat19(float float19) {
        this.float19 = float19;
    }

    public float getFloat2() {
        return float2;
    }

    public void setFloat2(float float2) {
        this.float2 = float2;
    }

    public float getFloat20() {
        return float20;
    }

    public void setFloat20(float float20) {
        this.float20 = float20;
    }

    public float getFloat21() {
        return float21;
    }

    public void setFloat21(float float21) {
        this.float21 = float21;
    }

    public float getFloat22() {
        return float22;
    }

    public void setFloat22(float float22) {
        this.float22 = float22;
    }

    public float getFloat23() {
        return float23;
    }

    public void setFloat23(float float23) {
        this.float23 = float23;
    }

    public float getFloat24() {
        return float24;
    }

    public void setFloat24(float float24) {
        this.float24 = float24;
    }

    public float getFloat25() {
        return float25;
    }

    public void setFloat25(float float25) {
        this.float25 = float25;
    }

    public float getFloat3() {
        return float3;
    }

    public void setFloat3(float float3) {
        this.float3 = float3;
    }

    public float getFloat4() {
        return float4;
    }

    public void setFloat4(float float4) {
        this.float4 = float4;
    }

    public float getFloat5() {
        return float5;
    }

    public void setFloat5(float float5) {
        this.float5 = float5;
    }

    public float getFloat6() {
        return float6;
    }

    public void setFloat6(float float6) {
        this.float6 = float6;
    }

    public float getFloat7() {
        return float7;
    }

    public void setFloat7(float float7) {
        this.float7 = float7;
    }

    public float getFloat8() {
        return float8;
    }

    public void setFloat8(float float8) {
        this.float8 = float8;
    }

    public float getFloat9() {
        return float9;
    }

    public void setFloat9(float float9) {
        this.float9 = float9;
    }

    public long getInteger1() {
        return integer1;
    }

    public void setInteger1(long integer1) {
        this.integer1 = integer1;
    }

    public long getInteger10() {
        return integer10;
    }

    public void setInteger10(long integer10) {
        this.integer10 = integer10;
    }

    public long getInteger11() {
        return integer11;
    }

    public void setInteger11(long integer11) {
        this.integer11 = integer11;
    }

    public long getInteger12() {
        return integer12;
    }

    public void setInteger12(long integer12) {
        this.integer12 = integer12;
    }

    public long getInteger13() {
        return integer13;
    }

    public void setInteger13(long integer13) {
        this.integer13 = integer13;
    }

    public long getInteger14() {
        return integer14;
    }

    public void setInteger14(long integer14) {
        this.integer14 = integer14;
    }

    public long getInteger15() {
        return integer15;
    }

    public void setInteger15(long integer15) {
        this.integer15 = integer15;
    }

    public long getInteger16() {
        return integer16;
    }

    public void setInteger16(long integer16) {
        this.integer16 = integer16;
    }

    public long getInteger17() {
        return integer17;
    }

    public void setInteger17(long integer17) {
        this.integer17 = integer17;
    }

    public long getInteger18() {
        return integer18;
    }

    public void setInteger18(long integer18) {
        this.integer18 = integer18;
    }

    public long getInteger19() {
        return integer19;
    }

    public void setInteger19(long integer19) {
        this.integer19 = integer19;
    }

    public long getInteger2() {
        return integer2;
    }

    public void setInteger2(long integer2) {
        this.integer2 = integer2;
    }

    public long getInteger20() {
        return integer20;
    }

    public void setInteger20(long integer20) {
        this.integer20 = integer20;
    }

    public long getInteger21() {
        return integer21;
    }

    public void setInteger21(long integer21) {
        this.integer21 = integer21;
    }

    public long getInteger22() {
        return integer22;
    }

    public void setInteger22(long integer22) {
        this.integer22 = integer22;
    }

    public long getInteger23() {
        return integer23;
    }

    public void setInteger23(long integer23) {
        this.integer23 = integer23;
    }

    public long getInteger24() {
        return integer24;
    }

    public void setInteger24(long integer24) {
        this.integer24 = integer24;
    }

    public long getInteger25() {
        return integer25;
    }

    public void setInteger25(long integer25) {
        this.integer25 = integer25;
    }

    public long getInteger3() {
        return integer3;
    }

    public void setInteger3(long integer3) {
        this.integer3 = integer3;
    }

    public long getInteger4() {
        return integer4;
    }

    public void setInteger4(long integer4) {
        this.integer4 = integer4;
    }

    public long getInteger5() {
        return integer5;
    }

    public void setInteger5(long integer5) {
        this.integer5 = integer5;
    }

    public long getInteger6() {
        return integer6;
    }

    public void setInteger6(long integer6) {
        this.integer6 = integer6;
    }

    public long getInteger7() {
        return integer7;
    }

    public void setInteger7(long integer7) {
        this.integer7 = integer7;
    }

    public long getInteger8() {
        return integer8;
    }

    public void setInteger8(long integer8) {
        this.integer8 = integer8;
    }

    public long getInteger9() {
        return integer9;
    }

    public void setInteger9(long integer9) {
        this.integer9 = integer9;
    }

    public String getText1() {
        return text1;
    }

    public void setText1(String text1) {
        this.text1 = text1;
    }

    public String getText10() {
        return text10;
    }

    public void setText10(String text10) {
        this.text10 = text10;
    }

    public String getText11() {
        return text11;
    }

    public void setText11(String text11) {
        this.text11 = text11;
    }

    public String getText12() {
        return text12;
    }

    public void setText12(String text12) {
        this.text12 = text12;
    }

    public String getText13() {
        return text13;
    }

    public void setText13(String text13) {
        this.text13 = text13;
    }

    public String getText14() {
        return text14;
    }

    public void setText14(String text14) {
        this.text14 = text14;
    }

    public String getText15() {
        return text15;
    }

    public void setText15(String text15) {
        this.text15 = text15;
    }

    public String getText16() {
        return text16;
    }

    public void setText16(String text16) {
        this.text16 = text16;
    }

    public String getText17() {
        return text17;
    }

    public void setText17(String text17) {
        this.text17 = text17;
    }

    public String getText18() {
        return text18;
    }

    public void setText18(String text18) {
        this.text18 = text18;
    }

    public String getText19() {
        return text19;
    }

    public void setText19(String text19) {
        this.text19 = text19;
    }

    public String getText2() {
        return text2;
    }

    public void setText2(String text2) {
        this.text2 = text2;
    }

    public String getText20() {
        return text20;
    }

    public void setText20(String text20) {
        this.text20 = text20;
    }

    public String getText21() {
        return text21;
    }

    public void setText21(String text21) {
        this.text21 = text21;
    }

    public String getText22() {
        return text22;
    }

    public void setText22(String text22) {
        this.text22 = text22;
    }

    public String getText23() {
        return text23;
    }

    public void setText23(String text23) {
        this.text23 = text23;
    }

    public String getText24() {
        return text24;
    }

    public void setText24(String text24) {
        this.text24 = text24;
    }

    public String getText25() {
        return text25;
    }

    public void setText25(String text25) {
        this.text25 = text25;
    }

    public String getText3() {
        return text3;
    }

    public void setText3(String text3) {
        this.text3 = text3;
    }

    public String getText4() {
        return text4;
    }

    public void setText4(String text4) {
        this.text4 = text4;
    }

    public String getText5() {
        return text5;
    }

    public void setText5(String text5) {
        this.text5 = text5;
    }

    public String getText6() {
        return text6;
    }

    public void setText6(String text6) {
        this.text6 = text6;
    }

    public String getText7() {
        return text7;
    }

    public void setText7(String text7) {
        this.text7 = text7;
    }

    public String getText8() {
        return text8;
    }

    public void setText8(String text8) {
        this.text8 = text8;
    }

    public String getText9() {
        return text9;
    }

    public void setText9(String text9) {
        this.text9 = text9;
    }

    public Date getDate1() {
        return date1;
    }

    public void setDate1(Date date1) {
        this.date1 = date1;
    }

    public Date getDate10() {
        return date10;
    }

    public void setDate10(Date date10) {
        this.date10 = date10;
    }

    public Date getDate11() {
        return date11;
    }

    public void setDate11(Date date11) {
        this.date11 = date11;
    }

    public Date getDate12() {
        return date12;
    }

    public void setDate12(Date date12) {
        this.date12 = date12;
    }

    public Date getDate13() {
        return date13;
    }

    public void setDate13(Date date13) {
        this.date13 = date13;
    }

    public Date getDate14() {
        return date14;
    }

    public void setDate14(Date date14) {
        this.date14 = date14;
    }

    public Date getDate15() {
        return date15;
    }

    public void setDate15(Date date15) {
        this.date15 = date15;
    }

    public Date getDate16() {
        return date16;
    }

    public void setDate16(Date date16) {
        this.date16 = date16;
    }

    public Date getDate17() {
        return date17;
    }

    public void setDate17(Date date17) {
        this.date17 = date17;
    }

    public Date getDate18() {
        return date18;
    }

    public void setDate18(Date date18) {
        this.date18 = date18;
    }

    public Date getDate19() {
        return date19;
    }

    public void setDate19(Date date19) {
        this.date19 = date19;
    }

    public Date getDate2() {
        return date2;
    }

    public void setDate2(Date date2) {
        this.date2 = date2;
    }

    public Date getDate20() {
        return date20;
    }

    public void setDate20(Date date20) {
        this.date20 = date20;
    }

    public Date getDate21() {
        return date21;
    }

    public void setDate21(Date date21) {
        this.date21 = date21;
    }

    public Date getDate22() {
        return date22;
    }

    public void setDate22(Date date22) {
        this.date22 = date22;
    }

    public Date getDate23() {
        return date23;
    }

    public void setDate23(Date date23) {
        this.date23 = date23;
    }

    public Date getDate24() {
        return date24;
    }

    public void setDate24(Date date24) {
        this.date24 = date24;
    }

    public Date getDate25() {
        return date25;
    }

    public void setDate25(Date date25) {
        this.date25 = date25;
    }

    public Date getDate3() {
        return date3;
    }

    public void setDate3(Date date3) {
        this.date3 = date3;
    }

    public Date getDate4() {
        return date4;
    }

    public void setDate4(Date date4) {
        this.date4 = date4;
    }

    public Date getDate5() {
        return date5;
    }

    public void setDate5(Date date5) {
        this.date5 = date5;
    }

    public Date getDate6() {
        return date6;
    }

    public void setDate6(Date date6) {
        this.date6 = date6;
    }

    public Date getDate7() {
        return date7;
    }

    public void setDate7(Date date7) {
        this.date7 = date7;
    }

    public Date getDate8() {
        return date8;
    }

    public void setDate8(Date date8) {
        this.date8 = date8;
    }

    public Date getDate9() {
        return date9;
    }

    public void setDate9(Date date9) {
        this.date9 = date9;
    }

    // ### END Generic getter and Setter methods ###

    public String[] getCategories() {
        return categories;
    }

    public void setCategories(String[] categories) {
        this.categories = categories;
    }

    public String getText_area1() {
        return text_area1;
    }

    public void setText_area1(String text_area1) {
        this.text_area1 = text_area1;
    }

    public String getText_area10() {
        return text_area10;
    }

    public void setText_area10(String text_area10) {
        this.text_area10 = text_area10;
    }

    public String getText_area11() {
        return text_area11;
    }

    public void setText_area11(String text_area11) {
        this.text_area11 = text_area11;
    }

    public String getText_area12() {
        return text_area12;
    }

    public void setText_area12(String text_area12) {
        this.text_area12 = text_area12;
    }

    public String getText_area13() {
        return text_area13;
    }

    public void setText_area13(String text_area13) {
        this.text_area13 = text_area13;
    }

    public String getText_area14() {
        return text_area14;
    }

    public void setText_area14(String text_area14) {
        this.text_area14 = text_area14;
    }

    public String getText_area15() {
        return text_area15;
    }

    public void setText_area15(String text_area15) {
        this.text_area15 = text_area15;
    }

    public String getText_area16() {
        return text_area16;
    }

    public void setText_area16(String text_area16) {
        this.text_area16 = text_area16;
    }

    public String getText_area17() {
        return text_area17;
    }

    public void setText_area17(String text_area17) {
        this.text_area17 = text_area17;
    }

    public String getText_area18() {
        return text_area18;
    }

    public void setText_area18(String text_area18) {
        this.text_area18 = text_area18;
    }

    public String getText_area19() {
        return text_area19;
    }

    public void setText_area19(String text_area19) {
        this.text_area19 = text_area19;
    }

    public String getText_area2() {
        return text_area2;
    }

    public void setText_area2(String text_area2) {
        this.text_area2 = text_area2;
    }

    public String getText_area20() {
        return text_area20;
    }

    public void setText_area20(String text_area20) {
        this.text_area20 = text_area20;
    }

    public String getText_area21() {
        return text_area21;
    }

    public void setText_area21(String text_area21) {
        this.text_area21 = text_area21;
    }

    public String getText_area22() {
        return text_area22;
    }

    public void setText_area22(String text_area22) {
        this.text_area22 = text_area22;
    }

    public String getText_area23() {
        return text_area23;
    }

    public void setText_area23(String text_area23) {
        this.text_area23 = text_area23;
    }

    public String getText_area24() {
        return text_area24;
    }

    public void setText_area24(String text_area24) {
        this.text_area24 = text_area24;
    }

    public String getText_area25() {
        return text_area25;
    }

    public void setText_area25(String text_area25) {
        this.text_area25 = text_area25;
    }

    public String getText_area3() {
        return text_area3;
    }

    public void setText_area3(String text_area3) {
        this.text_area3 = text_area3;
    }

    public String getText_area4() {
        return text_area4;
    }

    public void setText_area4(String text_area4) {
        this.text_area4 = text_area4;
    }

    public String getText_area5() {
        return text_area5;
    }

    public void setText_area5(String text_area5) {
        this.text_area5 = text_area5;
    }

    public String getText_area6() {
        return text_area6;
    }

    public void setText_area6(String text_area6) {
        this.text_area6 = text_area6;
    }

    public String getText_area7() {
        return text_area7;
    }

    public void setText_area7(String text_area7) {
        this.text_area7 = text_area7;
    }

    public String getText_area8() {
        return text_area8;
    }

    public void setText_area8(String text_area8) {
        this.text_area8 = text_area8;
    }

    public String getText_area9() {
        return text_area9;
    }

    public void setText_area9(String text_area9) {
        this.text_area9 = text_area9;
    }


	public String getDisabledWysiwyg() {
		return disabledWysiwyg;
	}

	public void setDisabledWysiwyg(String disabledWysiwyg) {
		this.disabledWysiwyg = disabledWysiwyg;
	}

	/*public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}*/

	/**
	 *
	 * @param f
	 * @param value
	 */
	public void setField(Field f, Object value) throws DotRuntimeException {
		try {
			if(value != null && value instanceof Timestamp){
				value = new Date(((Timestamp)value).getTime());
			}
			if(value!=null && value instanceof String && ((String)value).indexOf("\\u")>-1) {
				value = ((String)value).replace("\\u", "${esc.b}u");
			}
			BeanUtils.setProperty(this, f.getFieldContentlet(), value);
		}catch(IllegalArgumentException iae){
			Logger.error(this, "Unable to set the contentlet field.");
			throw new DotRuntimeException("Unable to set the contentlet field.", iae);
		} catch (IllegalAccessException e) {
			Logger.error(this, "Unable to set the contentlet field.");
			throw new DotRuntimeException("Unable to set the contentlet field.", e);
		} catch (InvocationTargetException e) {
			Logger.error(this, "Unable to set the contentlet field.");
			throw new DotRuntimeException("Unable to set the contentlet field.", e);
		}
	}


	/**
	 *
	 * @param fieldVarName velocityVarName
	 * @param value
	 * @throws DotRuntimeException if the field doesn't exist or it's not accesible
	 */
	public void setField(String fieldVarName, Object value) throws DotRuntimeException {
		Structure st = StructureCache.getStructureByInode(this.structureInode);
		Field f = st.getFieldVar(fieldVarName);
		if(f == null)
			throw new DotRuntimeException("Field: " + fieldVarName + " doesn't exist.");
		setField(f, value);
	}

	/**
	 * Returns a map of the contentlet properties based on the fields of the structure
	 * The keys used in the map will be the velocity variables names
	 */
	public Map<String, Object> getMap() throws DotRuntimeException {
		Map<String, Object> myMap = new HashMap<String, Object>();
		List<Field> fields = FieldsCache.getFieldsByStructureInode(structureInode);
		for (Field f : fields) {
			if(!APILocator.getFieldAPI().valueSettable(f)){
				continue;
			}
			if (Field.FieldType.HOST_OR_FOLDER.toString().equals(f.getFieldType())) {
				continue;
			}
			// http://jira.dotmarketing.net/browse/DOTCMS-1073
			// skip binary
			/*if(f.getFieldContentlet().startsWith("binary")){
				continue;
			}*/

			Object value;
			if(fAPI.isElementConstant(f)){
				value = f.getValues();
			}else{
				try {
					value = PropertyUtils.getProperty(this, f.getFieldContentlet());
					// http://jira.dotmarketing.net/browse/DOTCMS-3463
					/*** THIS LOGIC IS DUPED IN THE CONTENTLETAPI.  IF YOU CHANGE HERE, CHANGE THERE **/
					if(f.getFieldContentlet().startsWith("binary")&& value == null){
						java.io.File binaryFile = null ;
						java.io.File binaryFilefolder = new java.io.File(APILocator.getFileAPI().getRealAssetPath()
								+ java.io.File.separator
								+ getInode().charAt(0)
								+ java.io.File.separator
								+ getInode().charAt(1)
								+ java.io.File.separator
								+ getInode()
								+ java.io.File.separator
								+ f.getVelocityVarName());
						if(binaryFilefolder.exists()){
							java.io.File[] files = binaryFilefolder.listFiles(new BinaryFileFilter());
							for (File file : files) {
								String path = file.getPath();
								if(path!=null && path.indexOf("temp")==-1) {
									binaryFile = file;
									break;
								}
							}
						}
						value = binaryFile;
					}
				} catch (Exception e) {
					Logger.error(this, "Unable to obtain contentlet property value for: " + f.getFieldContentlet(), e);
					throw new DotRuntimeException("Unable to obtain contentlet property value for: " + f.getFieldContentlet(), e);
				}
			}
			myMap.put(f.getVelocityVarName(), value);

		}
		return myMap;
	}

	/**
	 * This method returns the value for any of the generic fields
	 * of the contentlet, given a fieldName using reflection, invoking the
	 * getter of the field.
	 * @param fieldName
	 * @return
	 */

	public Object getFieldValueByContentletName(String fieldName){

		Object value = null;
		try{
			value = org.apache.commons.beanutils.PropertyUtils.getProperty(this, fieldName);
		}catch(Exception e){
			Logger
			.error(this,
					"An error has ocurred trying to get the value for the field: " + fieldName );
		}
		return value;
	}

	public int compareTo(Object compObject) {
        if (!(compObject instanceof Contentlet))
            return -1;
        Contentlet contentlet = (Contentlet) compObject;
        if(contentlet!=null && contentlet.getTitle()!=null && this.getTitle()!=null) { // EE-3683
        	return (contentlet.getTitle().compareTo(this.getTitle()));
        } else {
        	return -1;
        }
    }
   //http://jira.dotmarketing.net/browse/DOTCMS-3463
	public File getBinary1() {
		return binary1;
	}
	public void setBinary1(File binary1) {
		this.binary1 = binary1;
	}
	public File getBinary2() {
		return binary2;
	}
	public void setBinary2(File binary2) {
		this.binary2 = binary2;
	}
	public File getBinary3() {
		return binary3;
	}
	public void setBinary3(File binary3) {
		this.binary3 = binary3;
	}
	public File getBinary4() {
		return binary4;
	}
	public void setBinary4(File binary4) {
		this.binary4 = binary4;
	}
	public File getBinary5() {
		return binary5;
	}
	public void setBinary5(File binary5) {
		this.binary5 = binary5;
	}
	public File getBinary6() {
		return binary6;
	}
	public void setBinary6(File binary6) {
		this.binary6 = binary6;
	}
	public File getBinary7() {
		return binary7;
	}
	public void setBinary7(File binary7) {
		this.binary7 = binary7;
	}
	public File getBinary8() {
		return binary8;
	}
	public void setBinary8(File binary8) {
		this.binary8 = binary8;
	}
	public File getBinary9() {
		return binary9;
	}
	public void setBinary9(File binary9) {
		this.binary9 = binary9;
	}
	public File getBinary10() {
		return binary10;
	}
	public void setBinary10(File binary10) {
		this.binary10 = binary10;
	}
	public File getBinary11() {
		return binary11;
	}
	public void setBinary11(File binary11) {
		this.binary11 = binary11;
	}
	public File getBinary12() {
		return binary12;
	}
	public void setBinary12(File binary12) {
		this.binary12 = binary12;
	}
	public File getBinary13() {
		return binary13;
	}
	public void setBinary13(File binary13) {
		this.binary13 = binary13;
	}
	public File getBinary14() {
		return binary14;
	}
	public void setBinary14(File binary14) {
		this.binary14 = binary14;
	}
	public File getBinary15() {
		return binary15;
	}
	public void setBinary15(File binary15) {
		this.binary15 = binary15;
	}
	public File getBinary16() {
		return binary16;
	}
	public void setBinary16(File binary16) {
		this.binary16 = binary16;
	}
	public File getBinary17() {
		return binary17;
	}
	public void setBinary17(File binary17) {
		this.binary17 = binary17;
	}
	public File getBinary18() {
		return binary18;
	}
	public void setBinary18(File binary18) {
		this.binary18 = binary18;
	}
	public File getBinary19() {
		return binary19;
	}
	public void setBinary19(File binary19) {
		this.binary19 = binary19;
	}
	public File getBinary20() {
		return binary20;
	}
	public void setBinary20(File binary20) {
		this.binary20 = binary20;
	}
	public File getBinary21() {
		return binary21;
	}
	public void setBinary21(File binary21) {
		this.binary21 = binary21;
	}
	public File getBinary22() {
		return binary22;
	}
	public void setBinary22(File binary22) {
		this.binary22 = binary22;
	}
	public File getBinary23() {
		return binary23;
	}
	public void setBinary23(File binary23) {
		this.binary23 = binary23;
	}
	public File getBinary24() {
		return binary24;
	}
	public void setBinary24(File binary24) {
		this.binary24 = binary24;
	}
	public File getBinary25() {
		return binary25;
	}
	public void setBinary25(File binary25) {
		this.binary25 = binary25;
	}
}
