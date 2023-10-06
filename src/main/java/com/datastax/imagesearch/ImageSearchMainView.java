package com.datastax.imagesearch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

import com.datastax.imagesearch.service.VectorImageController;
import com.datastax.imagesearch.service.VectorImageRepository;
import com.datastax.imagesearch.service.VectorImage;
import com.datastax.oss.driver.api.core.data.CqlVector;
import com.google.common.io.Files;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;

@Route("")
public class ImageSearchMainView extends VerticalLayout {

	private static final long serialVersionUID = -8942948333605499016L;
	
	private TextField queryField = new TextField();
	private Button queryButton;
	private Button upButton;
	private Image image = new Image();
	
	private EmbeddingModel embeddingModel;
	private Long imageCount = 0L;
	private MemoryBuffer buffer;
	private String noImageFile = "images/noImage.png";
	private StreamResource noImgFileStream;
	private Upload upload;
	
	private VectorImageController controller;
	
	public ImageSearchMainView(VectorImageRepository iRepo) {
		
		controller = new VectorImageController(iRepo);

		// run only once at startup
		imageCount = controller.getImageCount();
		
		// define CLIP model
		// model
        embeddingModel = HuggingFaceEmbeddingModel.builder()
                .accessToken(System.getenv("HF_API_KEY"))
                .modelId("sentence-transformers/clip-ViT-B-32")
                .waitForModel(true)
                .timeout(Duration.ofSeconds(60))
                .build();
		
		// query bar and button
		queryButton = new Button("Query");
		queryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		queryButton.addClickListener(click -> {
			String imageName = search(queryField.getValue());
			getImage(imageName);
		});
		Icon search = new Icon(VaadinIcon.SEARCH);
		queryField.setPrefixComponent(search);
		queryField.setWidth(800, Unit.PIXELS);
		add(queryField);
		add(queryButton);

		add(buildImageUpdateControls());
		add(buildImageData());
	}
	
	private Component buildImageData() {
		HorizontalLayout layout = new HorizontalLayout();
		
		try {
			FileInputStream fileStream = new FileInputStream(new File(noImageFile));
			noImgFileStream = new StreamResource("image",() -> {
				return fileStream;
			});
			image.setSrc(noImgFileStream);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		getImage("noImage.png");
		image.setHeight("300px");
		layout.add(image);
		
		return layout;
	}
	
	private Component buildImageUpdateControls() {
		HorizontalLayout layout = new HorizontalLayout();

		upButton = new Button("Upload JPG or PNG");
		upButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		buffer = new MemoryBuffer();
		upload = new Upload(buffer);
		upload.setUploadButton(upButton);
		upload.setAcceptedFileTypes("image/png", "image/jpg", "image/jpeg");
		upload.addSucceededListener(event -> {
			InputStream inStream = buffer.getInputStream();
			try {
				// get file from memory
				byte[] byteBuffer = new byte[inStream.available()];
				inStream.read(byteBuffer);
				
				// write to disk
				String filename = event.getFileName();
				File destination = new File("images/" + filename);
				Files.write(byteBuffer, destination);
				
				// generate vector embedding
				CqlVector<Float> vector = generateImageEmbedding(byteBuffer);
				
				Long imageCountPlusOne = imageCount + 1;
				
				// write to database
				controller.storeImageData(Integer.parseInt(imageCountPlusOne.toString()),
						filename, "description" + (imageCountPlusOne), vector);

				// increment image count once all is said and done
				imageCount++;

			} catch (IOException e) {
				e.printStackTrace();
			}
			
			upload.clearFileList();
		});

		layout.add(upload);

		return layout;
	}
	
	private void getImage(String imageName) {
		StreamResource src = getImageStream(imageName);
		image.setSrc(src);
	}
	
	private StreamResource getImageStream(String imageName) {
		StringBuilder filename = new StringBuilder("images/"); 
		
		if (!imageName.isEmpty()) {
			filename.append(imageName);

			if (new File(filename.toString()).exists()) {
			
				try {
					FileInputStream imgFileStream = new FileInputStream(new File(filename.toString()));
					StreamResource src = new StreamResource("image",() -> {
						return imgFileStream;
					});
					return src;
				} catch (FileNotFoundException ex) {
					// file not found; set to "No Image" file stream
					return noImgFileStream;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		
		return noImgFileStream;
	}
	
	private CqlVector<Float> generateImageEmbedding(byte[] byteBuffer) {
		
		Embedding vEmbedding = embeddingModel.embed(byteBuffer.toString());

		return CqlVector.newInstance(vEmbedding.vectorAsList());
	}
	
	private String search(String searchQuery) {
		
		Embedding vEmbedding = embeddingModel.embed(searchQuery);
		CqlVector<Float> vector = CqlVector.newInstance(vEmbedding.vectorAsList());
		VectorImage image = controller.getImageByVector(vector).getBody();
		
		return image.getName();
	}
}
