package com.datastax.imagesearch.service;

import java.util.List;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import com.datastax.oss.driver.api.core.data.CqlVector;

@Repository
public interface VectorImageRepository extends CassandraRepository<VectorImage,Integer>{

	@Query("SELECT name, description, item_vector FROM images ORDER BY item_vector ANN OF ?0 LIMIT 1")
	List<VectorImage> findImagesByVector(CqlVector<Float> vector);
}
