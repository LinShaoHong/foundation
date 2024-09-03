package com.github.sun.foundation.mybatis.checker;

import com.github.sun.foundation.modelling.Model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TableDef {
    public final String schema;
    public final Model model;
    public final List<Index> indexes;

    public List<String> primaryKey() {
        return model.primaryProperties().stream()
                .map(Model.Property::column)
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TableDef tableDef = (TableDef) o;
        return Objects.equals(schema, tableDef.schema) &&
                Objects.equals(model.tableName(), tableDef.model.tableName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, model.tableName());
    }

    public TableDef(String schema, Model model, List<Index> indexes) {
        this.schema = schema;
        this.model = model;
        this.indexes = indexes;
    }

    public static class Index {
        public final String name;
        public final List<String> keys;

        public Index(String name, List<String> keys) {
            this.name = name;
            this.keys = keys;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Index index = (Index) o;
            return name.equals(index.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
