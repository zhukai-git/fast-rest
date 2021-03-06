package com.zhukai.framework.fast.rest.jdbc.data.jpa;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.zhukai.framework.fast.rest.annotation.jpa.Column;
import com.zhukai.framework.fast.rest.annotation.jpa.Entity;
import com.zhukai.framework.fast.rest.annotation.jpa.GeneratedValue;
import com.zhukai.framework.fast.rest.annotation.jpa.Id;
import com.zhukai.framework.fast.rest.util.ReflectUtil;

public class JpaUtil {

	public static String getTableName(Class clazz) {
		String tableName = "";
		if (clazz.isAnnotationPresent(Entity.class)) {
			tableName = ((Entity) clazz.getAnnotation(Entity.class)).name();
		}
		tableName = tableName.equals("") ? clazz.getSimpleName().toLowerCase() : tableName;
		return tableName;
	}

	public static String getColumnName(Class clazz, String fieldName) {
		Field field = ReflectUtil.getDeclaredField(clazz, fieldName);
		return getColumnName(field);
	}

	public static String convertToSqlColumn(Field field) {
		StringBuilder sqlColumn = new StringBuilder();
		if (field.isAnnotationPresent(Column.class)) {
			Column column = field.getAnnotation(Column.class);
			String columnName = getColumnName(field);
			sqlColumn.append(columnName).append(" ");
			String columnType = getSqlType(field.getType());
			sqlColumn.append(columnType);
			if ("VARCHAR".equals(columnType)) {
				sqlColumn.append("(").append(column.length()).append(")");
			}
			if (!column.nullable()) {
				sqlColumn.append(" NOT NULL ");
			}
			if (column.unique()) {
				sqlColumn.append(" UNIQUE ");
			}
		} else {
			sqlColumn.append(field.getName()).append(" ");
			String columnType = getSqlType(field.getType());
			sqlColumn.append(columnType);
			if ("VARCHAR".equals(columnType)) {
				sqlColumn.append("(").append(255).append(")");
			}
		}
		if (field.isAnnotationPresent(Id.class)) {
			sqlColumn.append(" PRIMARY KEY ");
		}
		if (field.isAnnotationPresent(GeneratedValue.class)) {
			sqlColumn.append(" AUTO_INCREMENT ");
		}
		sqlColumn.append(",");
		if (field.getType().isAnnotationPresent(Entity.class)) {
			sqlColumn.append("FOREIGN KEY(").append(getColumnName(field)).append(") REFERENCES ");
			String tableName = getTableName(field.getType());
			Field idField = Arrays.stream(field.getType().getDeclaredFields()).filter(e -> e.isAnnotationPresent(Id.class)).findFirst().get();
			String idFieldName = getColumnName(idField);
			sqlColumn.append(tableName).append("(").append(idFieldName).append(")").append(",");
		}
		return sqlColumn.toString();
	}

	static Object convertToColumnValue(Object obj) {
		if (obj == null) {
			return null;
		}
		Object objValue;
		if (!obj.getClass().isAnnotationPresent(Entity.class)) {
			objValue = obj;
		} else {
			Field idField = getIdField(obj.getClass());
			objValue = getColumnValueByField(obj, idField);
		}
		if (objValue instanceof String) {
			return "'" + objValue + "'";
		}
		return objValue;

	}

	static Field getIdField(Class clazz) {
		return Arrays.stream(clazz.getDeclaredFields()).filter(e -> e.isAnnotationPresent(Id.class)).findFirst().get();
	}

	static String getColumnName(Field field) {
		String columnName = "";
		if (field.isAnnotationPresent(Column.class)) {
			columnName = field.getAnnotation(Column.class).name();
		}
		columnName = columnName.equals("") ? field.getName() : columnName;
		return columnName;
	}

	static StringBuilder getSelectSqlWithoutProperties(Class clazz) {
		return getSelectMainSql(clazz).append(getJoinSql(clazz));
	}

	static <T> T convertToEntity(Class<T> convertClazz, ResultSet resultSet) throws Exception {
		String mainTableName = getTableName(convertClazz);
		T entity = ReflectUtil.createInstance(convertClazz, null);
		for (Field field : convertClazz.getDeclaredFields()) {
			Object columnValue;
			if (field.getType().isAnnotationPresent(Entity.class)) {
				columnValue = convertToEntity(field.getType(), resultSet);
			} else {
				columnValue = resultSet.getObject(mainTableName + "." + getColumnName(field));
			}
			if (field.isAnnotationPresent(Id.class) && columnValue == null) {
				return null;
			}
			ReflectUtil.setFieldValue(entity, field.getName(), columnValue);
		}
		return entity;
	}

	static String getSelectSQL(Class clazz, Object[] properties) throws Exception {
		StringBuilder sql = new StringBuilder();
		sql.append(getSelectSqlWithoutProperties(clazz));
		if (properties != null) {
			sql.append(" WHERE ");
			for (int i = 0; i < properties.length; i += 2) {
				String columnName = properties[i].toString();
				String[] arr = columnName.split("\\.");
				Class fieldClass = clazz;
				String columnTableName = getTableName(clazz);
				for (int j = 0; j < arr.length - 1; j++) {
					Field field = ReflectUtil.getDeclaredField(fieldClass, arr[i]);
					if (field == null)
						continue;
					fieldClass = field.getType();
					columnTableName = getTableName(fieldClass);
				}
				sql.append(columnTableName).append(".").append(getColumnName(fieldClass, arr[arr.length - 1]));
				sql.append("=").append(convertToColumnValue(properties[i + 1])).append(" AND ");
			}
			sql.delete(sql.length() - 4, sql.length() - 1);
		}
		return sql.toString();
	}

	static <T> String getSaveSQL(T bean) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO ");
		String tableName = getTableName(bean.getClass());
		sql.append(tableName);
		StringBuilder columns = new StringBuilder("(");
		StringBuilder values = new StringBuilder("(");
		for (Field field : bean.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(GeneratedValue.class)) {
				continue;
			}
			String columnName = getColumnName(field);
			columns.append(columnName).append(",");
			Object columnValue = getColumnValueByField(bean, field);
			columnValue = convertToColumnValue(columnValue);
			values.append(columnValue).append(",");
		}
		columns.deleteCharAt(columns.length() - 1);
		columns.append(")");
		values.deleteCharAt(values.length() - 1);
		values.append(")");
		sql.append(columns).append(" VALUES ").append(values);
		return sql.toString();
	}

	static <T> String getUpdateSQL(T bean) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE ");
		String tableName = getTableName(bean.getClass());
		sql.append(tableName).append(" SET ");
		for (Field field : bean.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Id.class)) {
				continue;
			}
			String columnName = getColumnName(field);
			sql.append(columnName).append("=");
			Object columnValue = getColumnValueByField(bean, field);
			columnValue = convertToColumnValue(columnValue);
			sql.append(columnValue);
			sql.append(",");
		}
		sql.deleteCharAt(sql.length() - 1);
		Field idField = getIdField(bean.getClass());
		String idFieldName = getColumnName(idField);
		Object id = ReflectUtil.getFieldValue(bean, idField.getName());
		sql.append(" WHERE ").append(idFieldName).append("=").append(convertToColumnValue(id));
		return sql.toString();
	}

	private static String getSqlType(Class typeClass) {
		if (typeClass.equals(Integer.class)) {
			return "INTEGER";
		}
		if (typeClass.equals(Long.class)) {
			return "BIGINT";
		}
		if (typeClass.equals(String.class)) {
			return "VARCHAR";
		}
		if (typeClass.equals(Double.class)) {
			return "DOUBLE";
		}
		if (typeClass.equals(Float.class)) {
			return "FLOAT";
		}
		if (typeClass.isAnnotationPresent(Entity.class)) {
			Field idField = getIdField(typeClass);
			return getSqlType(idField.getType());
		}
		return null;
	}

	private static Object getColumnValueByField(Object obj, Field field) {
		Object fieldValue = ReflectUtil.getFieldValue(obj, field.getName());
		if (fieldValue == null) {
			return null;
		}
		if (fieldValue.getClass().isAnnotationPresent(Entity.class)) {
			Field idField = getIdField(fieldValue.getClass());
			return getColumnValueByField(fieldValue, idField);
		}
		return fieldValue;
	}

	private static List<Field> getJoinFields(Class clazz) {
		List<Field> joinFields = new ArrayList<>();
		for (Field field : clazz.getDeclaredFields()) {
			if (field.getType().isAnnotationPresent(Entity.class)) {
				joinFields.add(field);
			}
		}
		return joinFields;
	}

	private static StringBuilder getJoinSql(Class clazz) {
		StringBuilder sql = new StringBuilder();
		List<Field> joinFields = getJoinFields(clazz);
		if (joinFields == null || joinFields.isEmpty()) {
			return sql;
		}
		String mainTableName = getTableName(clazz);
		for (Field joinField : joinFields) {
			String joinTableName = getTableName(joinField.getType());
			String foreignKeyName = getColumnName(joinField);
			sql.append(" LEFT JOIN ").append(joinTableName).append(" ON ").append(mainTableName).append(".").append(foreignKeyName).append("=").append(joinTableName).append(".").append(getColumnName(getIdField(joinField.getType())))
					.append(" ");
			sql.append(getJoinSql(joinField.getType()));
		}
		return sql;
	}

	private static StringBuilder getSelectMainSql(Class clazz) {
		StringBuilder sql = new StringBuilder();
		String mainTableName = getTableName(clazz);
		sql.append("SELECT * FROM ").append(mainTableName).append(" ");
		return sql;
	}

}
