package com.dotmarketing.portlets.contentlet.transform;

import com.dotcms.util.ConversionUtils;
import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.portlets.contentlet.business.Contentlet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.NotNull;

/**
 * DBTransformer that converts DB objects into Contentlet instances
 */
public class FatContentletTransformer implements DBTransformer {
    final List<Contentlet> list;


    public FatContentletTransformer(final List<Map<String, Object>> initList){
        final List<Contentlet> newList = new ArrayList<>();
        if (initList != null){
            for(final Map<String, Object> map : initList){
                newList.add(transform(map));
            }
        }

        this.list = newList;
    }

    @Override
    public List<Contentlet> asList() {
        return this.list;
    }

    @NotNull
    private static Contentlet transform(final Map<String, Object> map)  {
        final Contentlet contentlet;
        contentlet = new Contentlet();
        contentlet.setInode((String) map.get("inode"));
        contentlet.setShowOnMenu(ConversionUtils.toBooleanFromDb(map.get("show_on_menu")));
        contentlet.setTitle((String) map.get("title"));
        contentlet.setModDate((Date) map.get("mod_date"));
        contentlet.setModUser((String) map.get("mod_user"));
        contentlet.setSortOrder(ConversionUtils.toInt(map.get("sort_order"),0));
        contentlet.setFriendlyName((String) map.get("friendly_name"));
        contentlet.setContentTypeId((String) map.get("structure_inode"));
        contentlet.setLastReview((Date) map.get("last_review"));
        contentlet.setNextReview((Date) map.get("next_review"));
        contentlet.setReviewInterval((String) map.get("review_interval"));
        contentlet.setDisabledWysiwyg((String) map.get("disable_wysiwyg"));
        contentlet.setIdentifier((String) map.get("identifier"));
        contentlet.setLanguageId(ConversionUtils.toLong(map.get("language_id"), 0L));

        // set date fields
        contentlet.setDate1((Date) map.get("date1"));
        contentlet.setDate2((Date) map.get("date2"));
        contentlet.setDate3((Date) map.get("date3"));
        contentlet.setDate4((Date) map.get("date4"));
        contentlet.setDate5((Date) map.get("date5"));
        contentlet.setDate6((Date) map.get("date6"));
        contentlet.setDate7((Date) map.get("date7"));
        contentlet.setDate8((Date) map.get("date8"));
        contentlet.setDate9((Date) map.get("date9"));
        contentlet.setDate10((Date) map.get("date10"));
        contentlet.setDate11((Date) map.get("date11"));
        contentlet.setDate12((Date) map.get("date12"));
        contentlet.setDate13((Date) map.get("date13"));
        contentlet.setDate14((Date) map.get("date14"));
        contentlet.setDate15((Date) map.get("date15"));
        contentlet.setDate16((Date) map.get("date16"));
        contentlet.setDate17((Date) map.get("date17"));
        contentlet.setDate18((Date) map.get("date18"));
        contentlet.setDate19((Date) map.get("date19"));
        contentlet.setDate20((Date) map.get("date20"));
        contentlet.setDate21((Date) map.get("date21"));
        contentlet.setDate22((Date) map.get("date22"));
        contentlet.setDate23((Date) map.get("date23"));
        contentlet.setDate24((Date) map.get("date24"));
        contentlet.setDate25((Date) map.get("date25"));

        // set text fields
        contentlet.setText1((String) map.get("text1"));
        contentlet.setText2((String) map.get("text2"));
        contentlet.setText3((String) map.get("text3"));
        contentlet.setText4((String) map.get("text4"));
        contentlet.setText5((String) map.get("text5"));
        contentlet.setText6((String) map.get("text6"));
        contentlet.setText7((String) map.get("text7"));
        contentlet.setText8((String) map.get("text8"));
        contentlet.setText9((String) map.get("text9"));
        contentlet.setText10((String) map.get("text10"));
        contentlet.setText11((String) map.get("text11"));
        contentlet.setText12((String) map.get("text12"));
        contentlet.setText13((String) map.get("text13"));
        contentlet.setText14((String) map.get("text14"));
        contentlet.setText15((String) map.get("text15"));
        contentlet.setText16((String) map.get("text16"));
        contentlet.setText17((String) map.get("text17"));
        contentlet.setText18((String) map.get("text18"));
        contentlet.setText19((String) map.get("text19"));
        contentlet.setText20((String) map.get("text20"));
        contentlet.setText21((String) map.get("text21"));
        contentlet.setText22((String) map.get("text22"));
        contentlet.setText23((String) map.get("text23"));
        contentlet.setText24((String) map.get("text24"));
        contentlet.setText25((String) map.get("text25"));

        // set text area fields
        contentlet.setText_area1((String) map.get("text_area1"));
        contentlet.setText_area2((String) map.get("text_area2"));
        contentlet.setText_area3((String) map.get("text_area3"));
        contentlet.setText_area4((String) map.get("text_area4"));
        contentlet.setText_area5((String) map.get("text_area5"));
        contentlet.setText_area6((String) map.get("text_area6"));
        contentlet.setText_area7((String) map.get("text_area7"));
        contentlet.setText_area8((String) map.get("text_area8"));
        contentlet.setText_area9((String) map.get("text_area9"));
        contentlet.setText_area10((String) map.get("text_area10"));
        contentlet.setText_area11((String) map.get("text_area11"));
        contentlet.setText_area12((String) map.get("text_area12"));
        contentlet.setText_area13((String) map.get("text_area13"));
        contentlet.setText_area14((String) map.get("text_area14"));
        contentlet.setText_area15((String) map.get("text_area15"));
        contentlet.setText_area16((String) map.get("text_area16"));
        contentlet.setText_area17((String) map.get("text_area17"));
        contentlet.setText_area18((String) map.get("text_area18"));
        contentlet.setText_area19((String) map.get("text_area19"));
        contentlet.setText_area20((String) map.get("text_area20"));
        contentlet.setText_area21((String) map.get("text_area21"));
        contentlet.setText_area22((String) map.get("text_area22"));
        contentlet.setText_area23((String) map.get("text_area23"));
        contentlet.setText_area24((String) map.get("text_area24"));
        contentlet.setText_area25((String) map.get("text_area25"));

        // set integer fields
        contentlet.setInteger1(ConversionUtils.toInt(map.get("integer1"),0));
        contentlet.setInteger2(ConversionUtils.toInt(map.get("integer2"),0));
        contentlet.setInteger3(ConversionUtils.toInt(map.get("integer3"),0));
        contentlet.setInteger4(ConversionUtils.toInt(map.get("integer4"),0));
        contentlet.setInteger5(ConversionUtils.toInt(map.get("integer5"),0));
        contentlet.setInteger6(ConversionUtils.toInt(map.get("integer6"),0));
        contentlet.setInteger7(ConversionUtils.toInt(map.get("integer7"),0));
        contentlet.setInteger8(ConversionUtils.toInt(map.get("integer8"),0));
        contentlet.setInteger9(ConversionUtils.toInt(map.get("integer9"),0));
        contentlet.setInteger10(ConversionUtils.toInt(map.get("integer10"),0));
        contentlet.setInteger11(ConversionUtils.toInt(map.get("integer11"),0));
        contentlet.setInteger12(ConversionUtils.toInt(map.get("integer12"),0));
        contentlet.setInteger13(ConversionUtils.toInt(map.get("integer13"),0));
        contentlet.setInteger14(ConversionUtils.toInt(map.get("integer14"),0));
        contentlet.setInteger15(ConversionUtils.toInt(map.get("integer15"),0));
        contentlet.setInteger16(ConversionUtils.toInt(map.get("integer16"),0));
        contentlet.setInteger17(ConversionUtils.toInt(map.get("integer17"),0));
        contentlet.setInteger18(ConversionUtils.toInt(map.get("integer18"),0));
        contentlet.setInteger19(ConversionUtils.toInt(map.get("integer19"),0));
        contentlet.setInteger20(ConversionUtils.toInt(map.get("integer20"),0));
        contentlet.setInteger21(ConversionUtils.toInt(map.get("integer21"),0));
        contentlet.setInteger22(ConversionUtils.toInt(map.get("integer22"),0));
        contentlet.setInteger23(ConversionUtils.toInt(map.get("integer23"),0));
        contentlet.setInteger24(ConversionUtils.toInt(map.get("integer24"),0));
        contentlet.setInteger25(ConversionUtils.toInt(map.get("integer25"),0));

        // set float fields
        contentlet.setFloat1(ConversionUtils.toFloat(map.get("float1"),0.0F));
        contentlet.setFloat2(ConversionUtils.toFloat(map.get("float2"),0.0F));
        contentlet.setFloat3(ConversionUtils.toFloat(map.get("float3"),0.0F));
        contentlet.setFloat4(ConversionUtils.toFloat(map.get("float4"),0.0F));
        contentlet.setFloat5(ConversionUtils.toFloat(map.get("float5"),0.0F));
        contentlet.setFloat6(ConversionUtils.toFloat(map.get("float6"),0.0F));
        contentlet.setFloat7(ConversionUtils.toFloat(map.get("float7"),0.0F));
        contentlet.setFloat8(ConversionUtils.toFloat(map.get("float8"),0.0F));
        contentlet.setFloat9(ConversionUtils.toFloat(map.get("float9"),0.0F));
        contentlet.setFloat10(ConversionUtils.toFloat(map.get("float10"),0.0F));
        contentlet.setFloat11(ConversionUtils.toFloat(map.get("float11"),0.0F));
        contentlet.setFloat12(ConversionUtils.toFloat(map.get("float12"),0.0F));
        contentlet.setFloat13(ConversionUtils.toFloat(map.get("float13"),0.0F));
        contentlet.setFloat14(ConversionUtils.toFloat(map.get("float14"),0.0F));
        contentlet.setFloat15(ConversionUtils.toFloat(map.get("float15"),0.0F));
        contentlet.setFloat16(ConversionUtils.toFloat(map.get("float16"),0.0F));
        contentlet.setFloat17(ConversionUtils.toFloat(map.get("float17"),0.0F));
        contentlet.setFloat18(ConversionUtils.toFloat(map.get("float18"),0.0F));
        contentlet.setFloat19(ConversionUtils.toFloat(map.get("float19"),0.0F));
        contentlet.setFloat20(ConversionUtils.toFloat(map.get("float20"),0.0F));
        contentlet.setFloat21(ConversionUtils.toFloat(map.get("float21"),0.0F));
        contentlet.setFloat22(ConversionUtils.toFloat(map.get("float22"),0.0F));
        contentlet.setFloat23(ConversionUtils.toFloat(map.get("float23"),0.0F));
        contentlet.setFloat24(ConversionUtils.toFloat(map.get("float24"),0.0F));
        contentlet.setFloat25(ConversionUtils.toFloat(map.get("float25"),0.0F));

        // set boolean values
        contentlet.setBool1(ConversionUtils.toBooleanFromDb(map.get("bool1")));
        contentlet.setBool2(ConversionUtils.toBooleanFromDb(map.get("bool2")));
        contentlet.setBool3(ConversionUtils.toBooleanFromDb(map.get("bool3")));
        contentlet.setBool4(ConversionUtils.toBooleanFromDb(map.get("bool4")));
        contentlet.setBool5(ConversionUtils.toBooleanFromDb(map.get("bool5")));
        contentlet.setBool6(ConversionUtils.toBooleanFromDb(map.get("bool6")));
        contentlet.setBool7(ConversionUtils.toBooleanFromDb(map.get("bool7")));
        contentlet.setBool8(ConversionUtils.toBooleanFromDb(map.get("bool8")));
        contentlet.setBool9(ConversionUtils.toBooleanFromDb(map.get("bool9")));
        contentlet.setBool10(ConversionUtils.toBooleanFromDb(map.get("bool10")));
        contentlet.setBool11(ConversionUtils.toBooleanFromDb(map.get("bool11")));
        contentlet.setBool12(ConversionUtils.toBooleanFromDb(map.get("bool12")));
        contentlet.setBool13(ConversionUtils.toBooleanFromDb(map.get("bool13")));
        contentlet.setBool14(ConversionUtils.toBooleanFromDb(map.get("bool14")));
        contentlet.setBool15(ConversionUtils.toBooleanFromDb(map.get("bool15")));
        contentlet.setBool16(ConversionUtils.toBooleanFromDb(map.get("bool16")));
        contentlet.setBool17(ConversionUtils.toBooleanFromDb(map.get("bool17")));
        contentlet.setBool18(ConversionUtils.toBooleanFromDb(map.get("bool18")));
        contentlet.setBool19(ConversionUtils.toBooleanFromDb(map.get("bool19")));
        contentlet.setBool20(ConversionUtils.toBooleanFromDb(map.get("bool20")));
        contentlet.setBool21(ConversionUtils.toBooleanFromDb(map.get("bool21")));
        contentlet.setBool22(ConversionUtils.toBooleanFromDb(map.get("bool22")));
        contentlet.setBool23(ConversionUtils.toBooleanFromDb(map.get("bool23")));
        contentlet.setBool24(ConversionUtils.toBooleanFromDb(map.get("bool24")));
        contentlet.setBool25(ConversionUtils.toBooleanFromDb(map.get("bool25")));

        return contentlet;
    }
}

