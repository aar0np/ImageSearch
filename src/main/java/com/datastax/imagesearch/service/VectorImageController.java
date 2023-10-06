package com.datastax.imagesearch.service;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datastax.oss.driver.api.core.data.CqlVector;

@RequestMapping("/imagesearch")
@RestController
public class VectorImageController {

		private VectorImageRepository imageRepo;
		
		public VectorImageController(VectorImageRepository iRepo) {
			imageRepo = iRepo;
		}
		
		@GetMapping("/images/vector/{vector}")
		public ResponseEntity<VectorImage> getImageByVector(@PathVariable(value="vector") CqlVector<Float> vector) {

			List<VectorImage> returnVal = imageRepo.findImagesByVector(vector);
			
			return ResponseEntity.ok(returnVal.getFirst());
		}
		
		public void storeImageData(int id, String name, String description, CqlVector<Float> vector) {
			VectorImage image = new VectorImage();
			image.setId(id);
			image.setName(name);
			image.setDescription(description);
			image.setItemVector(vector);
			
			imageRepo.save(image);
		}
		
		public long getImageCount() {
			return imageRepo.count();
		}
}
