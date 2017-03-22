package com.dotcms.contenttype.transform.field;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.IFieldVar;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.UtilMethods;


public class FieldVariableTransformer {

    final List<com.dotmarketing.portlets.structure.model.FieldVariable> oldVars;
    final List<FieldVariable> newVars;

    public FieldVariableTransformer(
            com.dotmarketing.portlets.structure.model.FieldVariable oldVar) {
        this(ImmutableList.of(oldVar));
    }

    public FieldVariableTransformer(FieldVariable newField) {
        this(ImmutableList.of(newField));
    }

    public FieldVariableTransformer(List<? extends IFieldVar> newVars) {

        List<FieldVariable> news = new ArrayList<FieldVariable>();
        List<com.dotmarketing.portlets.structure.model.FieldVariable> olds =
                new ArrayList<com.dotmarketing.portlets.structure.model.FieldVariable>();

        for (IFieldVar var : newVars) {
            if (var instanceof FieldVariable) {
                olds.add(transformToOld((FieldVariable) var));
                news.add((FieldVariable) var);
            } else {
                olds.add((com.dotmarketing.portlets.structure.model.FieldVariable) var);
                news.add(transformToNew(
                        (com.dotmarketing.portlets.structure.model.FieldVariable) var));
            }
        }

        this.newVars = ImmutableList.copyOf(news);
        this.oldVars = ImmutableList.copyOf(olds);
    }

    public FieldVariable newfield() throws DotStateException {
        if (this.newVars.size() == 0)
            throw new DotStateException("0 results");
        return this.newVars.get(0);
    }


    public List<FieldVariable> newFieldList() throws DotStateException {
        return this.newVars;
    }

    private static com.dotmarketing.portlets.structure.model.FieldVariable transformToOld(
            FieldVariable var) {

        com.dotmarketing.portlets.structure.model.FieldVariable fvar =
                new com.dotmarketing.portlets.structure.model.FieldVariable();
        fvar.setFieldId(var.fieldId());
        fvar.setId(var.id());
        fvar.setKey(var.key());
        fvar.setName(var.name());
        fvar.setValue(var.value());
        fvar.setLastModDate(var.modDate());
        fvar.setLastModifierId(var.userId());
        return fvar;

    }

    private static FieldVariable transformToNew(
            com.dotmarketing.portlets.structure.model.FieldVariable oldVar) {

        
        
        String id = (UtilMethods.isSet(oldVar.getId()))
                ? oldVar.getId() 
                        : FieldVariable.NOT_PERSISTED;

        return ImmutableFieldVariable.builder()
                .fieldId( oldVar.getFieldId())
                .id(id)
                .key(oldVar.getKey())
                .name(oldVar.getName())
                .modDate(oldVar.getLastModDate())
                .userId(oldVar.getLastModifierId())
                .value(oldVar.getValue())
                .build();
    }


    public com.dotmarketing.portlets.structure.model.FieldVariable oldField()
            throws DotStateException {
        return oldVars.get(0);
    }


    public List<com.dotmarketing.portlets.structure.model.FieldVariable> oldFieldList()
            throws DotStateException {
        return oldVars;
    }

}
