package com.datastax.imagesearch.service;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import com.datastax.oss.driver.api.core.data.CqlVector;

@Table("images")
public class VectorImage {

	@PrimaryKey("id")
	private int id;
	private String name;
	private String description;
	@Column("item_vector")
	private CqlVector<Float> itemVector;
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public CqlVector<Float> getItemVector() {
		return itemVector;
	}

	public void setItemVector(CqlVector<Float> itemVector) {
		this.itemVector = itemVector;
	}
}
