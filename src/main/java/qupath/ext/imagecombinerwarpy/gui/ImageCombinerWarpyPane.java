/*-
 * #%L
 * This file is part of QuPath.
 * %%
 * Copyright (C) 2018 - 2020 QuPath developers, The University of Edinburgh
 * %%
 * QuPath is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * QuPath is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with QuPath.  If not, see <https://www.gnu.org/licenses/>.
 * #L%
 */

package qupath.ext.imagecombinerwarpy.gui;

import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.TermCriteria;
import org.bytedeco.opencv.global.opencv_video;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.Indexer;
import org.controlsfx.control.CheckListView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Affine;
import javafx.scene.transform.MatrixType;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.TransformChangedEvent;
import javafx.stage.Stage;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSerializer;
import qupath.ext.imagecombinerwarpy.gui.InterpolationModes.InterpolationType;
import qupath.lib.color.ColorDeconvolutionStains;
import qupath.lib.display.ChannelDisplayInfo;
import qupath.lib.display.ImageDisplay;
import qupath.lib.geom.Point2;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.tools.GuiTools;
import qupath.lib.gui.tools.PaneTools;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.images.ImageData;
import qupath.lib.images.ImageData.ImageType;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.LabeledImageServer;
import qupath.lib.images.servers.PixelCalibration;
import qupath.lib.images.servers.PixelType;
import qupath.lib.images.servers.ServerTools;
import qupath.lib.images.servers.TransformedServerBuilder;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.objects.classes.PathClassFactory;
import qupath.lib.projects.Project;
import qupath.lib.projects.ProjectImageEntry;
import qupath.lib.regions.RegionRequest;
import qupath.lib.roi.GeometryTools;
import qupath.lib.scripting.QP;
import qupath.opencv.tools.OpenCVTools;


/**
 * A user interface for interacting with multiple image overlays.
 * 
 * @author Pete Bankhead
 *
 * modified by @phaub , 10'2021 (Support of viewer display settings)
 * 
 */

/*********************************
 * This file is part of ImageCombinerWarpy ...
 * 
 * .. a QuPath extension based on the QuPath 'Interactive Image Alignment' tool
 *  
 * The ImageCombinerWarpy is thought as an experimental� tool.
 * 
 * It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * Note: This file is derived from QuPath Image Align Extension and was added to this project and modified by @phaub (Oct 2021).
 *
 * Peter Haub (@phaub), Oct 2021
 * 
 *********************************/

public class ImageCombinerWarpyPane {
	
	private static Logger logger = LoggerFactory.getLogger(ImageCombinerWarpyPane.class);
	
	private QuPathGUI qupath;
	private QuPathViewer viewer;
	
	private ObservableList<ImageData<BufferedImage>> images = FXCollections.observableArrayList();
	private ObjectProperty<ImageData<BufferedImage>> selectedImageData = new SimpleObjectProperty<>();
	private DoubleProperty rotationIncrement = new SimpleDoubleProperty(1.0);		

	private DoubleProperty scaleIncrement = new SimpleDoubleProperty(1.0);
	private ArrayList<ImageDisplay> displays;
	private ArrayList<String> imageIDs;
	private ArrayList<String> warpyFileNames;
	private ArrayList<String> warpyFilePaths;

	private StringProperty affineStringProperty;
	
	private static enum RegistrationType {
		AFFINE, RIGID;

		@Override
		public String toString() {
			switch(this) {
			case AFFINE:
				return "Affine transform";
			case RIGID:
				return "Rigid transform";
			}
			throw new IllegalArgumentException("Unknown registration type " + this);
		}
	}
	
	private ObjectProperty<RegistrationType> registrationType = new SimpleObjectProperty<>(RegistrationType.AFFINE);
	
	
	private static enum AlignmentMethod {
			INTENSITY, AREA_ANNOTATIONS, POINT_ANNOTATIONS;
		
		@Override
		public String toString() {
			switch(this) {
			case INTENSITY:
				return "Image intensity";
			case AREA_ANNOTATIONS:
				return "Area annotations";
			case POINT_ANNOTATIONS:
				return "Point annotations";
			}
			throw new IllegalArgumentException("Unknown alignment method " + this);
		}
	}
	
	private ObjectProperty<AlignmentMethod> alignmentMethod = new SimpleObjectProperty<>(AlignmentMethod.INTENSITY);

	
	private ObjectProperty<InterpolationType> interpolationType = new SimpleObjectProperty<>(InterpolationType.NEARESTNEIGHBOR);
	
	
	private Map<ImageData<BufferedImage>, ImageCombinerWarpyServerOverlay> mapOverlays = new WeakHashMap<>();
	private EventHandler<TransformChangedEvent> transformEventHandler = new EventHandler<TransformChangedEvent>() {
		@Override
		public void handle(TransformChangedEvent event) {
			affineTransformUpdated();
		}
	};
	
	private RefineTransformMouseHandler mouseEventHandler = new RefineTransformMouseHandler();
	
	private ObjectBinding<ImageCombinerWarpyServerOverlay> selectedOverlay = Bindings.createObjectBinding(
			() -> {
				return mapOverlays.get(selectedImageData.get());
			},
			selectedImageData);
	
	private BooleanBinding noOverlay = selectedOverlay.isNull();

	private BooleanBinding noWarpy = Bindings.createBooleanBinding(
			() -> {
				return (!allWarpyFilesExist() || noOverlay.get());
			},
			selectedImageData); 
			
	private StringBinding selectedImageID = Bindings.createStringBinding(
			() -> {
				int idx = images.indexOf(selectedImageData.get());
				if (imageIDs.size() > 0 && idx >= 0 && idx < imageIDs.size() )
					return imageIDs.get(idx);
				return "--";
			},
			selectedImageData);
	
	private StringBinding selectedWarpyFileName = Bindings.createStringBinding(
			() -> {
				int idx = images.indexOf(selectedImageData.get());
				if (warpyFileNames.size() > 0 && idx >= 0 && idx < warpyFileNames.size())
					return warpyFileNames.get(idx);
				return "--";
			},
			selectedImageData);

	/**
	 * Constructor.
	 * @param qupath QuPath instance
	 */
	public ImageCombinerWarpyPane(final QuPathGUI qupath) {
		
		//::dip
		initDislayList();
		initWarpyInfoLists();
		
		this.qupath = qupath;
		this.viewer = qupath.getViewer();

		// ImageCombiner can not work with 32bit float images (yet)
		PixelType ptype = viewer.getImageData().getServer().getPixelType(); 
		if (ptype == PixelType.FLOAT32) {
			Dialogs.showErrorMessage("ImageCombinerWarpy", "ImageCombiner can not work with 32bit float images.");
			return;
		}

		this.viewer.getView().addEventFilter(MouseEvent.ANY, mouseEventHandler);
		
		// Create left-hand pane for list
		CheckListView<ImageData<BufferedImage>> listImages = new CheckListView<>(images);
		listImages.setPrefHeight(300);
		listImages.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		listImages.setCellFactory(c -> new ImageEntryCell());
		
		selectedImageData.bind(listImages.getSelectionModel().selectedItemProperty());
		
		Button btnChooseImages = new Button("Choose images from project");
		btnChooseImages.disableProperty().bind(qupath.projectProperty().isNull());
		btnChooseImages.setMaxWidth(Double.MAX_VALUE);
		btnChooseImages.setOnAction(e -> promptToAddImages());
		GridPane.setHgrow(btnChooseImages, Priority.ALWAYS);

		FloatProperty opacityProperty = viewer.getOverlayOptions().opacityProperty();
		Slider sliderOpacity = new Slider(0, 1, opacityProperty.get());
		sliderOpacity.valueProperty().bindBidirectional(opacityProperty);
		sliderOpacity.setMaxWidth(Double.MAX_VALUE);
		Label labelOpacity = new Label("Opacity");
		labelOpacity.setLabelFor(sliderOpacity);

		GridPane paneList = new GridPane();
		paneList.add(listImages, 0, 0, 2, 1);
		paneList.add(btnChooseImages, 0, 1, 2, 1);
		paneList.add(labelOpacity, 0, 2);
		paneList.add(sliderOpacity, 1, 2);
		paneList.setVgap(5);
		paneList.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillHeight(listImages, Boolean.TRUE);
		GridPane.setFillWidth(listImages, Boolean.TRUE);
		GridPane.setFillWidth(btnChooseImages, Boolean.TRUE);
		GridPane.setFillWidth(sliderOpacity, Boolean.TRUE);
		GridPane.setHgrow(listImages, Priority.ALWAYS);
		GridPane.setHgrow(btnChooseImages, Priority.ALWAYS);
		GridPane.setHgrow(sliderOpacity, Priority.ALWAYS);
		GridPane.setVgrow(listImages, Priority.ALWAYS);

		// Create center pane for alignment

		// Handle rotation
		TextField tfRotationIncrement = new TextField("1");
		tfRotationIncrement.setPrefColumnCount(6);
		tfRotationIncrement.textProperty().addListener((v, o, n) -> {
			if (!n.isEmpty()) {
				try {
					rotationIncrement.set(Double.parseDouble(n));
				} catch (Exception e) {}
			}
		});
		Label labelRotationIncrement = new Label("Rotation increment: ");
		labelRotationIncrement.setLabelFor(tfRotationIncrement);
		Button btnRotateLeft = new Button("Rotate Left");
		Button btnRotateRight = new Button("Rotate Right");
		btnRotateLeft.setOnAction(e -> requestRotation(rotationIncrement.get()));
		btnRotateRight.setOnAction(e -> requestRotation(-rotationIncrement.get()));	

		btnRotateLeft.disableProperty().bind(noOverlay);
		btnRotateRight.disableProperty().bind(noOverlay);
		
		//::dip
		// Handle scaling
		TextField tfScaleIncrement = new TextField("1");
		tfScaleIncrement.setPrefColumnCount(6);
		tfScaleIncrement.textProperty().addListener((v, o, n) -> {
			if (!n.isEmpty()) {
				try {
					scaleIncrement.set(Double.parseDouble(n));
				} catch (Exception e) {}
			}
		});
		Label labelScaleIncrement = new Label("Scale increment: ");
		labelScaleIncrement.setLabelFor(tfRotationIncrement);
		Button btnScaleDown = new Button("Scale Down");
		Button btnScaleUp = new Button("Scale Up");
		btnScaleDown.setOnAction(e -> requestScaling(scaleIncrement.get()));			
		btnScaleUp.setOnAction(e -> requestScaling(1.0/scaleIncrement.get()));

		btnScaleUp.disableProperty().bind(noOverlay);
		btnScaleDown.disableProperty().bind(noOverlay);

		GridPane paneAlignment = new GridPane();
		paneAlignment.setHgap(5);
		paneAlignment.setVgap(5);
		int row = 0;
		int col = 0;
		
		Label labelTranslate = new Label("Adjust translation by clicking & dragging on the image with the 'Shift' key down");
		labelTranslate.setAlignment(Pos.CENTER);
		labelTranslate.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(labelTranslate, Priority.ALWAYS);
		GridPane.setFillWidth(labelTranslate, Boolean.TRUE);
		paneAlignment.add(labelTranslate, 0, row++, 5, 1);

		ComboBox<InterpolationType> comboInterpolation = new ComboBox<>(
				FXCollections.observableArrayList(InterpolationType.values()));
		comboInterpolation.setMaxWidth(Double.MAX_VALUE);
		comboInterpolation.getSelectionModel().select(interpolationType.get());
		interpolationType.bind(comboInterpolation.getSelectionModel().selectedItemProperty());
		Label labelInterpolationType = new Label("Interpolation Mode:");
		
		paneAlignment.add(labelRotationIncrement, col++, row);
		paneAlignment.add(tfRotationIncrement, col++, row);
		paneAlignment.add(btnRotateLeft, col++, row);
		paneAlignment.add(btnRotateRight, col++, row);
		paneAlignment.add(labelInterpolationType,  col++, row++);
		//::dip
		col = 0;
		paneAlignment.add(labelScaleIncrement, col++, row);
		paneAlignment.add(tfScaleIncrement, col++, row);
		paneAlignment.add(btnScaleDown, col++, row);
		paneAlignment.add(btnScaleUp, col++, row);
		paneAlignment.add(comboInterpolation, col++, row++);
		
		TitledPane titledAlignment = new TitledPane("Interactive alignment", paneAlignment);
		
		// Auto-align
		GridPane paneAutoAlign = new GridPane();
		row = 0;
		col = 0;

		Label labelAuto = new Label("Auto-alignment may work better if the images have been coarsely aligned interactively");
		paneAutoAlign.add(labelAuto, col, row++, 2, 1);

		ComboBox<RegistrationType> comboRegistration = new ComboBox<>(
				FXCollections.observableArrayList(RegistrationType.values()));
		comboRegistration.setMaxWidth(Double.MAX_VALUE);
		comboRegistration.getSelectionModel().select(registrationType.get());
		registrationType.bind(comboRegistration.getSelectionModel().selectedItemProperty());
		Label labelRegistrationType = new Label("Registration type");
		paneAutoAlign.add(labelRegistrationType, 0, row);
		paneAutoAlign.add(comboRegistration, 1, row++);
		GridPane.setFillWidth(comboRegistration, Boolean.TRUE);
		
		ComboBox<AlignmentMethod> comboAlign = new ComboBox<>(
				FXCollections.observableArrayList(AlignmentMethod.values()));
		comboAlign.setMaxWidth(Double.MAX_VALUE);
		comboAlign.getSelectionModel().select(alignmentMethod.get());
		alignmentMethod.bind(comboAlign.getSelectionModel().selectedItemProperty());
		Label labelAlignmentType = new Label("Alignment type");
		paneAutoAlign.add(labelAlignmentType, 0, row);
		paneAutoAlign.add(comboAlign, 1, row++);
		GridPane.setFillWidth(comboAlign, Boolean.TRUE);
		
		TextField tfRequestedPixelSizeMicrons = new TextField("20");
		tfRequestedPixelSizeMicrons.setPrefColumnCount(6);
		Label labelRequestedPixelSizeMicrons = new Label("Pixel size");
		Button btnAutoAlign = new Button("Estimate transform");
		btnAutoAlign.setMaxWidth(Double.MAX_VALUE);
		btnAutoAlign.disableProperty().bind(noOverlay);
		btnAutoAlign.setOnAction(e -> {
			double requestedPixelSizeMicrons = Double.parseDouble(tfRequestedPixelSizeMicrons.getText());
			try {
				autoAlign(requestedPixelSizeMicrons);
			} catch (IOException e2) {
				Dialogs.showErrorMessage("Alignment error", "Error requesting image region: " + e2.getLocalizedMessage());
				logger.error("Error in auto alignment", e2);
			}
		});
//		var paramsAuto = new ParameterList()
//				.addChoiceParameter("alignmentType", "Alignment type", alignmentType.get(), align);
		paneAutoAlign.add(labelRequestedPixelSizeMicrons, 0, row);
		paneAutoAlign.add(tfRequestedPixelSizeMicrons, 1, row++);
		
		paneAutoAlign.add(btnAutoAlign, 0, row++, 2, 1);
//		paneAutoAlign.add(btnAutoAlign, 0, 1, 3, 1);
		paneAutoAlign.setVgap(5);
		paneAutoAlign.setHgap(5);
		TitledPane titledAutoAlign = new TitledPane("Auto-align", paneAutoAlign);

		// Warpy Info
		GridPane paneWarpy = new GridPane();
		row = 0;

		// Show Warpy spline transform info
		TextField tfImageID = new TextField("--");
		tfImageID.setEditable(false);
		tfImageID.setPrefColumnCount(4);
		Label labelImageID = new Label("Image ID: ");
		labelImageID.setLabelFor(tfImageID);
		paneWarpy.add(labelImageID, 0, row, 1, 1);
		paneWarpy.add(tfImageID, 1, row, 1, 1);
		
		TextField tfWarpyFileName = new TextField("--");
		tfWarpyFileName.setEditable(false);
		tfWarpyFileName.setPrefColumnCount(15);
		Label labelWarpyFileName = new Label("Warpy file: ");
		labelWarpyFileName.setLabelFor(tfWarpyFileName);
		paneWarpy.add(labelWarpyFileName, 2, row, 1, 1);
		paneWarpy.add(tfWarpyFileName, 3, row++, 3, 1);
		
		tfImageID.textProperty().bind(selectedImageID);
		tfWarpyFileName.textProperty().bind(selectedWarpyFileName);

		paneWarpy.setVgap(5.0);		
		paneWarpy.setHgap(5.0);		
		TitledPane titledWarpy = new TitledPane("Warpy Info", paneWarpy);
		
		// Show affine transform
		GridPane paneTransform = new GridPane();
		row = 0;

		TextArea textArea = new TextArea();
		affineStringProperty = textArea.textProperty();
		textArea.setPrefRowCount(2);
		paneTransform.add(new Label("Current affine transform being displayed"), 0, row++);
		paneTransform.add(textArea, 0, row++, 2, 1);
		
		Button btnUpdate = new Button("Update");
		btnUpdate.setOnAction(e -> {
			var overlay = getSelectedOverlay();
			var affine = overlay == null ? null : overlay.getAffine();
			if (affine == null)
				return;
			try {
				// Parse String as AffineTransform
				var newAffine = GeometryTools.parseTransformMatrix(textArea.getText());
				var values = newAffine.getMatrixEntries();

				// JavaFX's Affine has a different element ordering than awt's AffineTransform
				affine.setToTransform(values[0], values[1], values[2], values[3], values[4], values[5]);
			} catch (ParseException ex) {
				Dialogs.showErrorMessage("Parse affine transform", "Unable to parse affine transform!");
				logger.error("Error parsing transform: " + ex.getLocalizedMessage(), ex);
			}
		});
		Button btnReset = new Button("Reset");
		btnReset.setOnAction(e -> {
			var overlay = getSelectedOverlay();
			var affine = overlay == null ? null : overlay.getAffine();
			if (affine == null)
				return;
			affine.setToIdentity();
		});
		Button btnInvert = new Button("Invert");
		btnInvert.setOnAction(e -> {
			var overlay = getSelectedOverlay();
			var affine = overlay == null ? null : overlay.getAffine();
			if (affine == null)
				return;
			try {
				affine.invert();
			} catch (NonInvertibleTransformException ex) {
				Dialogs.showErrorNotification("Invert transform", "Transform not invertable!");
				logger.error("Error trying to invert affine transform: " + ex.getLocalizedMessage(), ex);
			};
		});
		Button btnCopy = new Button("Copy");
		btnCopy.setOnAction(e -> {
			var overlay = getSelectedOverlay();
			var affine = overlay == null ? null : overlay.getAffine();
			if (affine == null) {
				logger.warn("No transform found, can't copy to clipboard!");
				return;
			}
			ClipboardContent content = new ClipboardContent();
			String s = affine.getElement(MatrixType.MT_2D_2x3, 0, 0) + ",\t" +
					affine.getElement(MatrixType.MT_2D_2x3, 0, 1) + ",\t" +
					affine.getElement(MatrixType.MT_2D_2x3, 0, 2) + "\n" +
					affine.getElement(MatrixType.MT_2D_2x3, 1, 0) + ",\t" +
					affine.getElement(MatrixType.MT_2D_2x3, 1, 1) + ",\t" +
					affine.getElement(MatrixType.MT_2D_2x3, 1, 2);
			content.putString(s);
			Clipboard.getSystemClipboard().setContent(content);
		});
		//::dip
		Button btnCreate = new Button("Create");
		btnCreate.setOnAction(e -> {			
			addProjectEntry(false);		
		});		
		Button btnCreateWarpy = new Button("Warpy");
		btnCreateWarpy.setOnAction(e -> {			
			addProjectEntry(true);		
		});
		btnReset.disableProperty().bind(noOverlay);
		btnReset.setTooltip(new Tooltip("Reset the transform"));
		btnInvert.disableProperty().bind(noOverlay);
		btnInvert.setTooltip(new Tooltip("Invert the transform"));
		btnUpdate.disableProperty().bind(noOverlay);
		btnUpdate.setTooltip(new Tooltip("Update the transform using the current text"));
		btnCopy.disableProperty().bind(noOverlay);
		btnCopy.setTooltip(new Tooltip("Copy the current transform to clipboard"));
		btnCreate.disableProperty().bind(noOverlay);
		btnCreate.setTooltip(new Tooltip("Create project entry"));
		btnCreateWarpy.disableProperty().bind(noOverlay);
		btnCreateWarpy.disableProperty().bind(noWarpy);	
		btnCreateWarpy.setTooltip(new Tooltip("Create Warpy project entry"));
		textArea.editableProperty().bind(noOverlay.not());
		paneTransform.add(PaneTools.createColumnGridControls(btnUpdate, btnInvert, btnReset, btnCopy, btnCreate, btnCreateWarpy), 0, row++);
		PaneTools.setFillWidth(Boolean.TRUE, paneTransform.getChildren().toArray(Node[]::new));
		PaneTools.setHGrowPriority(Priority.ALWAYS, paneTransform.getChildren().toArray(Node[]::new));
		paneTransform.setVgap(5.0);
		
		TitledPane titledTransform = new TitledPane("Transform", paneTransform);

		
		// Need to update transform text with image
		selectedImageData.addListener((v, o, n) -> affineTransformUpdated());

//		Accordion paneMain = new Accordion(
//				titledAlignment,
//				titledAutoAlign);
		
		titledAlignment.setCollapsible(false);
		titledAutoAlign.setCollapsible(false);
		titledTransform.setCollapsible(false);
		VBox paneMain = new VBox(titledAlignment, titledAutoAlign, titledWarpy, titledTransform);

		// Show only the current overlay on the viewer
		selectedOverlay.addListener((v, o, n) -> {
			if (o != null)
				viewer.getCustomOverlayLayers().remove(o);
			if (n != null)
				viewer.getCustomOverlayLayers().add(n);
		});

		// Bring panes together
		TitledPane titledList = new TitledPane("Image & overlays", paneList);
		titledList.setCollapsible(false);
		SplitPane pane = new SplitPane(titledList, paneMain);
		pane.setDividerPositions(0.35);


		// Add current image to list, if we have one
		ImageData<BufferedImage> imageDataCurrent = viewer.getImageData();
		if (imageDataCurrent != null) {
			listImages.getItems().add(imageDataCurrent);
		}


		Stage stage = new Stage();
		if (qupath != null)
			stage.initOwner(qupath.getStage());
		stage.setTitle("Image Combiner Warpy (experimental)");

		Scene scene = new Scene(pane);
		stage.setScene(scene);

		stage.show();
		
		
		stage.setOnHiding(e -> {
			// Remove event filter & any overlays we created
			this.viewer.getView().removeEventFilter(MouseEvent.ANY, mouseEventHandler);
			this.viewer.getCustomOverlayLayers().removeAll(mapOverlays.values());
		});
		
	}


	void addProjectEntry(boolean useWarpyTransformation) {
		String msg = "Add combined image to project (Affine Transformation with Interpolation)?";
		if ( useWarpyTransformation )
			msg = "Add combined image to project (Warpy Transformation)?";
			
		if (!Dialogs.showYesNoDialog("Add project entry", msg))
			return;

		boolean allWarpyFilesExist = allWarpyFilesExist();				
		if (useWarpyTransformation && !allWarpyFilesExist) {
			Dialogs.showErrorMessage("ImageCombinerWarpy", "No valid channel selection in base image. Operation aborted");
			return;
		}
				
		Project<BufferedImage> project = qupath.getProject();
		
		ArrayList<ImageChannel> channels = new ArrayList<ImageChannel>();
		
		ImageData<BufferedImage> imageDataBase = viewer.getImageData();
		ImageServer<BufferedImage> serverBase = imageDataBase.getServer();
		ProjectImageEntry<BufferedImage> entryBase = project.getEntry(imageDataBase);
		String name1 = entryBase.getImageName();
		ImageDisplay display1 = viewer.getImageDisplay();

		// Add base image 
		
		// Channel selection added for baseImage
		
		ObservableList<ChannelDisplayInfo> selectedChannelInfos = display1.selectedChannels();
		if (selectedChannelInfos == null || selectedChannelInfos.size() == 0) {
			Dialogs.showErrorMessage("ImageCombinerWarpy", "No channel selected in base image. Operation aborted");
			return;
		}
		
		boolean useRGBChannelOnly = false;
		if (imageDataBase.isBrightfield()) {
			for (ChannelDisplayInfo info : selectedChannelInfos) {
				if (info.getName().equals("Red") || info.getName().equals("Green") ||
					info.getName().equals("Blue")) {
						useRGBChannelOnly = true;
						break;
				}
			}
		}
		List<String> selectedChannelNames = new ArrayList<String>();
		for (ChannelDisplayInfo info : selectedChannelInfos) {
			if (imageDataBase.isBrightfield()) {
				if (useRGBChannelOnly && (info.getName().equals("Red") || 
					info.getName().equals("Green") || info.getName().equals("Blue")))
						selectedChannelNames.add(info.getName());
				else if (info.getName().equals("Original")){
					selectedChannelNames.add("Red");
					selectedChannelNames.add("Green");
					selectedChannelNames.add("Blue");
				}
			}
			else
				selectedChannelNames.add(info.getName());
		}

		List<String> selectedChannelShortNames = new ArrayList<String>();
		List<ImageChannel> selectedChannels = new ArrayList<ImageChannel>();
		List<ImageChannel> currentChannels = serverBase.getMetadata().getChannels();
		for (ImageChannel channel : currentChannels) {
			String tmpChnName = channel.getName();
			boolean isSelected = containsChannelName(selectedChannelNames, tmpChnName);
			if (isSelected) {
				selectedChannels.add(channel);
				selectedChannelShortNames.add(tmpChnName);
			}
		}
		channels.addAll(updateChannelNames(name1, selectedChannels));
		
		// Add base image channels
		
		ImageServer<BufferedImage> serverStart = null;
		if (selectedChannelShortNames.size() != 0) {
			TransformedServerBuilder builder1 = new TransformedServerBuilder(serverBase);
			String[] selectedChn = (String[]) selectedChannelShortNames.toArray(new String[0]);		
			builder1.extractChannels(selectedChn);
			serverStart = builder1.build();
		}
		
		if (imageDataBase.isBrightfield() && !useRGBChannelOnly) {  //selectedChannels.size() == 0) {
			ColorDeconvolutionStains stains = imageDataBase.getColorDeconvolutionStains();
			TransformedServerBuilder cdbuilder = new TransformedServerBuilder(serverBase);
			if (stains != null) {
				List<Integer> stainNumbers = new ArrayList<>();
				for (int s = 1; s <= 3; s++) {
					if (!stains.getStain(s).isResidual())
						stainNumbers.add(s);
				}
				cdbuilder.deconvolveStains(stains, stainNumbers.stream().mapToInt(n -> n).toArray());
			}			
			ImageServer<BufferedImage> colorDeconServer = cdbuilder.build();

			if (serverStart != null) {
				TransformedServerBuilder builder = new TransformedServerBuilder(serverStart);
				builder.concatChannels(colorDeconServer);
				ImageServer<BufferedImage> serverCDTmp = builder.build();
	
				serverBase = serverCDTmp;
			}
			else {
				serverBase = colorDeconServer;
			}
			
			ArrayList<ImageChannel> channelsCD = new ArrayList<ImageChannel>();
			for (int k=0; k<colorDeconServer.nChannels(); k++) 
				channelsCD.add(colorDeconServer.getChannel(k));				
			channels.addAll(updateChannelNames(name1, channelsCD));			
		}
		else
			serverBase = serverStart;
		
		if (serverBase == null) {
			Dialogs.showErrorMessage("ImageCombinerWarpy", "No valid channel selection in base image. Operation aborted");
			return;
		}

		// End: Add base image 

		// Create transformed overlay images servers
		
		ArrayList<ImageServer<BufferedImage>> transformServers = new ArrayList<ImageServer<BufferedImage>>();
		
		for (int i=0; i<images.size()-1; i++) { // the base image (last in images) has no ImageCombinerWarpyServerOverlay
			ImageData<BufferedImage> imageDataTmp = images.get(i);
			
			ImageCombinerWarpyServerOverlay overlayTmp = mapOverlays.get(imageDataTmp);
			
			if (overlayTmp != null) { // ... it is an overlay image
				ImageServer<BufferedImage> serverTmp = imageDataTmp.getServer();

				PixelType ptype = serverTmp.getPixelType(); 
				if (ptype == PixelType.FLOAT32) {
					Dialogs.showErrorMessage("ImageCombinerWarpy", "ImageCombiner can not work with 32bit float images.");
					return;
				}

				ImageDisplay displayTmp = null;
				if (displays != null)
					displayTmp = displays.get(i);
				
				String nameTmp = "";
				ProjectImageEntry<BufferedImage> entryTmp = project.getEntry(imageDataTmp);

				// It's not always guaranteed to receive an entry. In this case get name from OriginalMetadata.
				if (entryTmp != null)
					nameTmp = entryTmp.getImageName();
				else
					nameTmp = serverTmp.getOriginalMetadata().getName();
				
				if (displayTmp == null || displayTmp.selectedChannels().size() == 0) {
					// Overlay image is not displayed in a viewer or no channel is selected 
					// => Add all channels
					
					channels.addAll(updateChannelNames(nameTmp, serverTmp.getMetadata().getChannels()));
					
					if (imageDataTmp.isBrightfield()) {
						ColorDeconvolutionStains stains = imageDataTmp.getColorDeconvolutionStains();
						TransformedServerBuilder cdbuilder = new TransformedServerBuilder(serverTmp);
						if (stains != null) {
							List<Integer> stainNumbers = new ArrayList<>();
							for (int s = 1; s <= 3; s++) {
								if (!stains.getStain(s).isResidual())
									stainNumbers.add(s);
							}
							cdbuilder.deconvolveStains(stains, stainNumbers.stream().mapToInt(n -> n).toArray());
						}			
						ImageServer<BufferedImage> colorDeconServer = cdbuilder.build();
	
						TransformedServerBuilder builder = new TransformedServerBuilder(serverTmp);
						builder.concatChannels(colorDeconServer);
						ImageServer<BufferedImage> serverCDTmp = builder.build();
	
						serverTmp = serverCDTmp;
						
						ArrayList<ImageChannel> channelsCD = new ArrayList<ImageChannel>();
						for (int k=0; k<colorDeconServer.nChannels(); k++) 
							channelsCD.add(colorDeconServer.getChannel(k));				
						channels.addAll(updateChannelNames(nameTmp, channelsCD));			
					}
				}
				else {
					// Display is available (overlay image is displayed in a viewer)
					// => Use channel selection for overlay images
					// ( This causes a change of the image type !?)
					
					ObservableList<ChannelDisplayInfo> selectedChannelInfosTmp = displayTmp.selectedChannels();
					
					boolean useRGBChannelOnlyTmp = false;
					if (imageDataTmp.isBrightfield()) {
						for (ChannelDisplayInfo info : selectedChannelInfosTmp) {
							if (info.getName().equals("Red") || info.getName().equals("Green") ||
								info.getName().equals("Blue")) {
									useRGBChannelOnlyTmp = true;
									break;
							}
						}
					}
					List<String> selectedChannelNamesTmp = new ArrayList<String>();
					for (ChannelDisplayInfo info : selectedChannelInfosTmp) {
						if (imageDataTmp.isBrightfield()) {
							if (useRGBChannelOnlyTmp && (info.getName().equals("Red") || 
								info.getName().equals("Green") || info.getName().equals("Blue")))
									selectedChannelNamesTmp.add(info.getName());
							else if (info.getName().equals("Original")){
								selectedChannelNamesTmp.add("Red");
								selectedChannelNamesTmp.add("Green");
								selectedChannelNamesTmp.add("Blue");
							}
						}
						else
							selectedChannelNamesTmp.add(info.getName());
					}

					List<String> selectedChannelShortNamesTmp = new ArrayList<String>();
					List<ImageChannel> selectedChannelsTmp = new ArrayList<ImageChannel>();
					List<ImageChannel> currentChannelsTmp = serverTmp.getMetadata().getChannels();
					for (ImageChannel channel : currentChannelsTmp) {
						String tmpChnName = channel.getName();
						boolean isSelected = containsChannelName(selectedChannelNamesTmp, tmpChnName);
						if (isSelected) {
							selectedChannelsTmp.add(channel);
							selectedChannelShortNamesTmp.add(tmpChnName);
						}
					}
					channels.addAll(updateChannelNames(nameTmp, selectedChannelsTmp));
										
					ImageServer<BufferedImage> serverStartTmp = null;
					if (selectedChannelShortNamesTmp.size() != 0) {
						TransformedServerBuilder builderTmp = new TransformedServerBuilder(serverTmp);
						String[] selectedChn = (String[]) selectedChannelShortNamesTmp.toArray(new String[0]);		
						builderTmp.extractChannels(selectedChn);
						serverStartTmp = builderTmp.build();
					}
					
					if (imageDataTmp.isBrightfield() && (!useRGBChannelOnlyTmp)){
						ColorDeconvolutionStains stains = imageDataTmp.getColorDeconvolutionStains();
						TransformedServerBuilder cdbuilder = new TransformedServerBuilder(serverTmp);
						if (stains != null) {
							List<Integer> stainNumbers = new ArrayList<>();
							for (int s = 1; s <= 3; s++) {
								if (!stains.getStain(s).isResidual())
									stainNumbers.add(s);
							}
							cdbuilder.deconvolveStains(stains, stainNumbers.stream().mapToInt(n -> n).toArray());
						}			
						ImageServer<BufferedImage> colorDeconServer = cdbuilder.build();

						if (serverStartTmp != null) {
							TransformedServerBuilder builder = new TransformedServerBuilder(serverStartTmp);
							builder.concatChannels(colorDeconServer);
							ImageServer<BufferedImage> serverCDTmp = builder.build();
				
							serverTmp = serverCDTmp;
						}
						else {
							serverTmp = colorDeconServer;
						}
						
						ArrayList<ImageChannel> channelsCD = new ArrayList<ImageChannel>();
						for (int k=0; k<colorDeconServer.nChannels(); k++) 
							channelsCD.add(colorDeconServer.getChannel(k));				
						channels.addAll(updateChannelNames(nameTmp, channelsCD));			
					}
					else {
						serverTmp = serverStartTmp;
					}
					
					if (serverTmp == null) {
						Dialogs.showErrorMessage("ImageCombinerWarpy", "No valid channel selection in overlay image. Operation aborted");
						return;
					}					
				}

				// Create the transformed server
				ImageServer<BufferedImage> transformServerTmp = null;
				
				if ( useWarpyTransformation ) {
					// Create overlay image with Warpy Spline Transformation
	
					String filePath =  warpyFilePaths.get(i);
					
					RealTransform realtransform = null;				
					File f = new File(filePath);
					if (f.exists()) {
						realtransform = getRealTransform( f );
					}
					else {
			        	System.out.println("No transformation file " + f.getName());
			        	return;
					}
									
					int interpolationMode = interpolationType.get().ordinal();
					RealTransformInterpolationSequence realtransformsequence = new RealTransformInterpolationSequence(realtransform, interpolationMode);
					try {
						transformServerTmp = new RealTransformImageServer(serverTmp, realtransformsequence, interpolationMode);
					} catch (NoninvertibleTransformException e) {
						e.printStackTrace();
					}
				}
				else {
					// Create overlay image with Affine Transformation
				
					Affine affine = overlayTmp.getAffine();			
					double[] m = affine.toArray(MatrixType.MT_2D_2x3);
					
					// Transformation has to be 'inverted'
					AffineTransform transform = new AffineTransform();
					transform.setTransform(m[0], m[3], m[1], m[4], 0, 0);
					try {
						transform.invert();
					} catch (NoninvertibleTransformException e) {
						e.printStackTrace();
					}
					transform.translate(-m[2], -m[5]);
				    
					int interpolationMode = interpolationType.get().ordinal();
					AffineTransformInterpolation transforminterpolate = new AffineTransformInterpolation(transform, interpolationMode);

					try {
						transformServerTmp = new AffineTransformInterpolationImageServer(serverTmp, transforminterpolate);
					} catch (NoninvertibleTransformException e) {
						e.printStackTrace();
					}
				}
				
				transformServers.add(transformServerTmp);
			}
		}

		// End: Create transformed overlay images servers

		System.out.println("n channels: " + channels.size());

		// Combine base image and transformed overlay images (Create combined/concatenated server)
		
		TransformedServerBuilder serverBuilderFinal = new TransformedServerBuilder(serverBase)
		                                    			.concatChannels(transformServers);

		ImageServer<BufferedImage> serverFinal = serverBuilderFinal.build();

		ImageServerMetadata newMetadata = new ImageServerMetadata.Builder(serverFinal.getMetadata())
											.name(name1 + "_Overlay")
											.build();
		
		serverFinal.setMetadata(newMetadata);
		
		// Create the new image
		ImageData<BufferedImage> imageDataCreated = new ImageData<BufferedImage>(serverFinal);
		
		ImageChannel[] channelsArray = new ImageChannel[channels.size()];
		for (int i=0; i<channels.size(); i++)
			channelsArray[i] = channels.get(i);
		
		QP.setChannels(imageDataCreated, channelsArray);
		imageDataCreated.setImageType(ImageType.FLUORESCENCE);

		// Add project entry and display the concatenated image
		// The following command forces displaying the new image in the current viewer 
		// and indirectly add a new project entry
		// ToDo: Check if there is a more clear and stable solution to add a new project entry.
		Platform.runLater(() -> QuPathGUI.getInstance().getViewer().setImageData(imageDataCreated));
		
	}
	
	
	public static RealTransform getRealTransform(File f) {
		FileReader fileReader = null;
		try {
		    fileReader = new FileReader(f.getAbsolutePath());
		    RealTransform rt = RealTransformSerializer.deserialize(fileReader, RealTransform.class);
		    fileReader.close();
		    return rt;
		} catch (FileNotFoundException e) {
		    System.out.println("Transform file " + f.getName() + " not found"); // , e);
		} catch (IOException e) {
			System.out.println("Error reading transform file " + f.getName()); //, e);
		}
		
		return null;
	}

	private boolean containsChannelName(List<String> channelName, String checkName) {
		for (String name : channelName) {
			if (name.indexOf(checkName) >= 0)
				return true;
		}
		return false;
	}

	
	//::dip
	// Prepend a base name to channel names
	List<ImageChannel> updateChannelNames(String name, List<ImageChannel> channels) {
		List<ImageChannel> newChannels = new ArrayList<ImageChannel>();
		for (int i=0; i<channels.size(); i++) {
			ImageChannel chn = channels.get(i);
			ImageChannel newChn = ImageChannel.getInstance(name + '-' + chn.getName(), chn.getColor());
			newChannels.add(newChn);			
		}
		return newChannels;
	}
	
	void parseAffine(String text, Affine affine) {
		String delims = "\t\n ";
		// If we have any periods, then use a comma as an acceptable delimiter as well
		if (text.contains("."))
			delims += ",";
		var tokens = new StringTokenizer(text, delims);
		if (tokens.countTokens() != 6) {
			Dialogs.showErrorMessage("Parse affine transform", "Affine transform should be tab-delimited and contain 6 numbers only");
			return;
		}
		var nf = NumberFormat.getInstance();
		try {
			double m00 = nf.parse(tokens.nextToken()).doubleValue();
			double m01 = nf.parse(tokens.nextToken()).doubleValue();
			double m02 = nf.parse(tokens.nextToken()).doubleValue();
			double m10 = nf.parse(tokens.nextToken()).doubleValue();
			double m11 = nf.parse(tokens.nextToken()).doubleValue();
			double m12 = nf.parse(tokens.nextToken()).doubleValue();
			affine.setToTransform(m00, m01, m02, m10, m11, m12);
		} catch (Exception e) {
			Dialogs.showErrorMessage("Parse affine transform", "Unable to parse affine transform!");
			logger.error("Error parsing transform: " + e.getLocalizedMessage(), e);
		}
	}
	
		
	void promptToAddImages() {
		// Get all the other project entries - except for the base image (which is fixed)
		Project<BufferedImage> project = qupath.getProject();
		List<ProjectImageEntry<BufferedImage>> entries = new ArrayList<>(project.getImageList());
		ImageData<BufferedImage> imageDataCurrent = viewer.getImageData();
		ProjectImageEntry<BufferedImage> currentEntry = project.getEntry(imageDataCurrent);
		if (currentEntry != null)
			entries.remove(currentEntry);
		
		// Find the entries currently selected
		Set<ProjectImageEntry<BufferedImage>> alreadySelected = 
				images.stream().map(i -> project.getEntry(i)).collect(Collectors.toSet());
		if (currentEntry != null)
			alreadySelected.remove(currentEntry);
		
		// Create a list to display, with the appropriate selections
		ListView<ProjectImageEntry<BufferedImage>>  list = new ListView<>();
		list.getItems().setAll(entries);
		list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		for (int i = 0; i < entries.size(); i++) {
			if (alreadySelected.contains(entries.get(i)))
				list.getSelectionModel().select(i);
		}
		
		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		dialog.setHeaderText("Select images to include");
		dialog.getDialogPane().setContent(list);
		Optional<ButtonType> result = dialog.showAndWait();
		
		if (result.orElse(ButtonType.CANCEL) == ButtonType.CANCEL)
			return;
		
		// We now need to add some & remove some (potentially)
		Set<ProjectImageEntry<BufferedImage>> toSelect = new LinkedHashSet<>(list.getSelectionModel().getSelectedItems());
		Set<ProjectImageEntry<BufferedImage>> toRemove = new HashSet<>(alreadySelected);
		toRemove.removeAll(toSelect);
		toSelect.removeAll(alreadySelected);
		
		// Rather convoluted... but remove anything that needs to go, from the list, map & overlay
		if (!toRemove.isEmpty()) {
			List<ImageData<BufferedImage>> imagesToRemove = new ArrayList<>();
			for (ImageData<BufferedImage> temp : images) {
				for (ProjectImageEntry<BufferedImage> entry : toRemove) {
					if (entry == currentEntry)
						imagesToRemove.add(temp);
				}
			}
			images.removeAll(imagesToRemove);
			for (ImageData<BufferedImage> temp : imagesToRemove) {
				ImageCombinerWarpyServerOverlay overlay = mapOverlays.remove(temp);
				if (overlay != null) {
					overlay.getAffine().removeEventHandler(TransformChangedEvent.ANY, transformEventHandler);
					viewer.getCustomOverlayLayers().remove(overlay);					
				}
			}
		}
		
		// Add any images that need to be added
		List<ImageData<BufferedImage>> imagesToAdd = new ArrayList<>();
		for (ProjectImageEntry<BufferedImage> temp : toSelect) {
			ImageData<BufferedImage> imageData = null;
			
			ImageDisplay displayTmp = null;
			//::dip Support of viewer display settings
			// Read annotations from any data file
			try {
				// Try to get data from an open viewer first, if possible
				for (var viewerTmp : qupath.getViewers()) {
					var tempData = viewerTmp.getImageData();
					if (tempData != null && temp.equals(project.getEntry(viewerTmp.getImageData()))) {
						imageData = tempData;
						//::dip Support of viewer display settings
						displayTmp = viewerTmp.getImageDisplay();
						break;
					}
				}
				// Read the data from the project if necessary
				if (imageData == null) {
					if (temp.hasImageData()) {
						imageData = temp.readImageData();
						// Remove non-annotations to save memory
						Collection<PathObject> pathObjects = imageData.getHierarchy().getObjects(null, null);
						Set<PathObject> pathObjectsToRemove = pathObjects.stream().filter(p -> !p.isAnnotation()).collect(Collectors.toSet());
						imageData.getHierarchy().removeObjects(pathObjectsToRemove, true);
					} else {
						// Read the data from the project (but without a data file we expect this to really create a new image)
						imageData = temp.readImageData();
					}
				}
			} catch (IOException e) {
				logger.error("Unable to read ImageData for " + temp.getImageName(), e);
				continue;
			}
			
			PixelType ptype = imageData.getServer().getPixelType(); 
			if (ptype == PixelType.FLOAT32) {
				Dialogs.showErrorMessage("ImageCombinerWarpy", "One of your selected images is 32bit type. ImageCombiner can not work with 32bit float images.");
				continue;
			}
			
			ImageCombinerWarpyServerOverlay overlay = new ImageCombinerWarpyServerOverlay(viewer, imageData.getServer());
			//::dip Support of viewer display settings
			overlay.setRenderer(displayTmp);
			
			overlay.getAffine().addEventHandler(TransformChangedEvent.ANY, transformEventHandler);
			mapOverlays.put(imageData, overlay);
			imagesToAdd.add(imageData);
		}
		images.addAll(0, imagesToAdd);
		
		getDisplaysList();
		getWarpyInfoList();
	}
	
	
	private void getDisplaysList() {
		initDislayList();

		for (int i=0; i<images.size(); i++) {
			ImageData<BufferedImage> imageData = images.get(i);			
			ImageCombinerWarpyServerOverlay overlayTmp = mapOverlays.get(imageData);
			
			if (overlayTmp != null) { 
				ImageDisplay displayTmp = null;
				// Try to get the viewer for the image
				for (var viewerTmp : qupath.getViewers()) {
					var viewerImageData = viewerTmp.getImageData();
					if (viewerImageData != null && imageData.equals(viewerImageData)) {
						displayTmp = viewerTmp.getImageDisplay();
						break;
					}
				}
				displays.add(displayTmp);	
			}
		}		
	}
	
	
	private void initDislayList() {
		displays = new ArrayList<ImageDisplay>();
	}
		
	private void getWarpyInfoList() {
		initWarpyInfoLists();

	    // Pattern to match the transform file
	    Pattern transformFilePattern = Pattern.compile("transform\\_(?<target>\\d+)\\_(?<source>\\d+)\\.json");

		Project<BufferedImage> project = qupath.getProject();
		ImageData<BufferedImage> imageDataBase = viewer.getImageData();
		ProjectImageEntry<BufferedImage> entryBase = project.getEntry(imageDataBase);
        String idBase = entryBase.getID();

		for (int i=0; i<images.size(); i++) {
			ImageData<BufferedImage> imageData = images.get(i);
			
			ProjectImageEntry<BufferedImage> entry = project.getEntry(imageData);

	        String imageID = "--";
	        String warpyFileName = "--";
	        String warpyFilePath = "";
	        
			// It's not always guaranteed to receive an entry.
			if (entry != null) {
		        Path entrypath= entry.getEntryPath();
		        imageID = entry.getID();
		        
				//Search for Warpy transformation file				
		        for (File currentFile : entrypath.toFile().listFiles()) {
		            Matcher matcher = transformFilePattern.matcher(currentFile.getName());
		            if (matcher.matches()) {
		                if (matcher.group("target").equals(imageID)
		                	&& matcher.group("source").equals(idBase)) {
		                    // Warpy transformation file found
		                	warpyFileName = currentFile.getName();
		                	warpyFilePath = currentFile.getPath();
		                	break;
		                }
		            }
		        }
			}
	        
			imageIDs.add(imageID);
			warpyFileNames.add(warpyFileName);
			warpyFilePaths.add(warpyFilePath);			
		}		
	}
	
	private void initWarpyInfoLists() {
		imageIDs = new ArrayList<String>();
		warpyFileNames =  new ArrayList<String>();
		warpyFilePaths =  new ArrayList<String>();
	}
		
	boolean allWarpyFilesExist() {
		// Check if warpy transformation files exist for all overlay images 
		boolean allWarpyFilesExist = false;		
		for (int i=0; i<images.size(); i++) {
			ImageData<BufferedImage> imageDataTmp = images.get(i);
			ImageCombinerWarpyServerOverlay overlayTmp = mapOverlays.get(imageDataTmp);
			
			if (overlayTmp != null) {
				if ( !warpyFilePaths.get(i).equals(""))
					allWarpyFilesExist = true;
				else {
					allWarpyFilesExist = false;
					break;					
				}
			}
		}
		return allWarpyFilesExist;
	}
	
	
	
	private ImageCombinerWarpyServerOverlay getSelectedOverlay() {
		return mapOverlays.get(selectedImageData.get());
	}
	
	private void affineTransformUpdated() {
		ImageCombinerWarpyServerOverlay overlay = getSelectedOverlay();
		if (overlay == null) {
			affineStringProperty.set("No overlay selected");
			return;
		}
		Affine affine = overlay.getAffine();
		affineStringProperty.set(
				String.format(
				"%.5f, \t %.5f,\t %.5f,\n" + 
				"%.5f,\t %.5f,\t %.5f",
				affine.getMxx(), affine.getMxy(), affine.getTx(),
				affine.getMyx(), affine.getMyy(), affine.getTy())
				);
	}
	
	
	
	/**
	 * Ensure an image is 8-bit grayscale, creating a new image if necessary.
	 * 
	 * @param img
	 * @return
	 */
	static BufferedImage ensureGrayScale(BufferedImage img) {
		if (img.getType() == BufferedImage.TYPE_BYTE_GRAY)
			return img;
		if (img.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
			 ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
             var colorModel = new ComponentColorModel(cs, new int[]{8}, false, true,
                                                  Transparency.OPAQUE,
                                                  DataBuffer.TYPE_BYTE);
			return new BufferedImage(colorModel, img.getRaster(), false, null);
		}
		BufferedImage imgGray = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g2d = imgGray.createGraphics();
		g2d.drawImage(img, 0, 0, null);
		g2d.dispose();
		return imgGray;
	}

	/**
	 * Auto-align the selected image overlay with the base image in the viewer.
	 * 
	 * @param requestedPixelSizeMicrons
	 * @throws IOException 
	 */
	void autoAlign(double requestedPixelSizeMicrons) throws IOException {
		ImageData<BufferedImage> imageDataBase = viewer.getImageData();
		ImageData<BufferedImage> imageDataSelected = selectedImageData.get();
		if (imageDataBase == null) {
			Dialogs.showNoImageError("Auto-alignment");
			return;
		}
		if (imageDataSelected == null) {
			Dialogs.showErrorMessage("Auto-alignment", "Please ensure an image overlay is selected!");
			return;
		}
		if (imageDataBase == imageDataSelected) {
			Dialogs.showErrorMessage("Auto-alignment", "Please select an image overlay, not the 'base' image from the viewer!");
			return;
		}
		ImageCombinerWarpyServerOverlay overlay = mapOverlays.get(imageDataSelected);
		
		var affine = overlay.getAffine();
		
		ImageServer<BufferedImage> serverBase, serverSelected;

		if (alignmentMethod.get() == AlignmentMethod.POINT_ANNOTATIONS) {
			logger.debug("Image alignment using point annotations");
			logger.warn("Point annotation alignment not yet implemented!");
			Mat transform;
			List<Point2> pointsBase = new ArrayList<>();
			List<Point2> pointsSelected = new ArrayList<>();
			for (var annotation : imageDataBase.getHierarchy().getAnnotationObjects()) {
				var roi = annotation.getROI();
				if (roi == null || roi.isArea())
					continue;
				pointsBase.addAll(roi.getAllPoints());
			}
			for (var annotation : imageDataSelected.getHierarchy().getAnnotationObjects()) {
				var roi = annotation.getROI();
				if (roi == null || roi.isArea())
					continue;
				pointsSelected.addAll(roi.getAllPoints());
			}
			if (pointsBase.isEmpty() && pointsSelected.isEmpty()) {
				Dialogs.showErrorMessage("Align images", "No points found for either image!");
				return;
			}
			if (pointsBase.size() != pointsSelected.size()) {
				Dialogs.showErrorMessage("Align images", "Images have different numbers of annotated points (" + pointsBase.size() + " & " + pointsSelected.size() + ")");
				return;				
			}
			Mat matBase = pointsToMat(pointsBase);
			Mat matSelected = pointsToMat(pointsSelected);
			
			transform = opencv_video.estimateRigidTransform(matBase, matSelected, registrationType.get() == RegistrationType.AFFINE);
//			if (registrationType.get() == RegistrationType.AFFINE)
//				transform = opencv_calib3d.estimateAffine2D(matBase, matSelected);
//			else
//				transform = opencv_calib3d.estimateAffinePartial2D(matBase, matSelected);
			matToAffine(transform, affine, 1.0);
			return;
		}
		
		if (alignmentMethod.get() == AlignmentMethod.AREA_ANNOTATIONS) {
			logger.debug("Image alignment using area annotations");
			Map<PathClass, Integer> labels = new LinkedHashMap<>();
			int label = 1;
			labels.put(PathClassFactory.getPathClassUnclassified(), label++);
			for (var annotation : imageDataBase.getHierarchy().getAnnotationObjects()) {
				var pathClass = annotation.getPathClass();
				if (pathClass != null && !labels.containsKey(pathClass))
					labels.put(pathClass, label++);
			}
			for (var annotation : imageDataSelected.getHierarchy().getAnnotationObjects()) {
				var pathClass = annotation.getPathClass();
				if (pathClass != null && !labels.containsKey(pathClass))
					labels.put(pathClass, label++);
			}
			
			double downsampleBase = requestedPixelSizeMicrons / imageDataBase.getServer().getPixelCalibration().getAveragedPixelSize().doubleValue();
			serverBase = new LabeledImageServer.Builder(imageDataBase)
				.backgroundLabel(0)
				.addLabels(labels)
				.downsample(downsampleBase)
				.build();
			
			double downsampleSelected = requestedPixelSizeMicrons / imageDataSelected.getServer().getPixelCalibration().getAveragedPixelSize().doubleValue();
			serverSelected = new LabeledImageServer.Builder(imageDataSelected)
					.backgroundLabel(0)
					.addLabels(labels)
					.downsample(downsampleSelected)
					.build();
			
		} else {
			// Default - just use intensities
			logger.debug("Image alignment using intensities");
			serverBase = imageDataBase.getServer();
			serverSelected = imageDataSelected.getServer();			
		}
		
		//autoAlign(serverBase, serverSelected, registrationType.get(), affine, requestedPixelSizeMicrons);
		
		//::dip
		int idxSelected = images.indexOf(imageDataSelected);
		ImageDisplay overlayDisplay = displays.get(idxSelected);

		autoAlign(serverBase, serverSelected, registrationType.get(), affine, requestedPixelSizeMicrons, viewer, overlayDisplay);
	}
	
	
	static Mat pointsToMat(Collection<Point2> points) {
		Mat mat = new Mat(points.size(), 2, opencv_core.CV_32FC1);
		int r = 0;
		FloatIndexer idx = mat.createIndexer();
		for (var p : points) {
			idx.put(r, 0, (float)p.getX());
			idx.put(r, 1, (float)p.getY());
			r++;
		}
		idx.release();
		return mat;
	}
	

	//::dip modified
	static void autoAlign(ImageServer<BufferedImage> serverBase, ImageServer<BufferedImage> serverOverlay, RegistrationType registrationType, Affine affine, double requestedPixelSizeMicrons, QuPathViewer viewerTmp, ImageDisplay overlayDisplay) throws IOException {
		PixelCalibration calBase = serverBase.getPixelCalibration();
		double pixelSize = calBase.getAveragedPixelSizeMicrons();
		double downsample = 1;
		if (!Double.isFinite(pixelSize)) {
			while (serverBase.getWidth() / downsample > 2000)
				downsample++;
			logger.warn("Pixel size is unavailable! Default downsample value of {} will be used", downsample);
		} else {
			downsample = requestedPixelSizeMicrons / calBase.getAveragedPixelSizeMicrons();			
		}

		BufferedImage imgBase = serverBase.readBufferedImage(RegionRequest.createInstance(serverBase.getPath(), downsample, 0, 0, serverBase.getWidth(), serverBase.getHeight()));
		BufferedImage imgOverlay = serverOverlay.readBufferedImage(RegionRequest.createInstance(serverOverlay.getPath(), downsample, 0, 0, serverOverlay.getWidth(), serverOverlay.getHeight()));
		
		//::dip
		if (viewerTmp != null)
			imgBase = viewerTmp.getImageDisplay().applyTransforms(imgBase, null);
		
		if (overlayDisplay != null)
			imgOverlay = overlayDisplay.applyTransforms(imgOverlay, null);
		
		imgBase = ensureGrayScale(imgBase);
		imgOverlay = ensureGrayScale(imgOverlay);
		
		Mat matBase = OpenCVTools.imageToMat(imgBase);
		Mat matOverlay = OpenCVTools.imageToMat(imgOverlay);
		
		// For testing
		//OpenCVTools.matToImagePlus("matBase", matBase).show();
		//OpenCVTools.matToImagePlus("matOverlay", matOverlay).show();
		
//		opencv_imgproc.threshold(matBase, matBase, opencv_imgproc.THRESH_OTSU, 255, opencv_imgproc.THRESH_BINARY_INV + opencv_imgproc.THRESH_OTSU);
//		opencv_imgproc.threshold(matOverlay, matOverlay, opencv_imgproc.THRESH_OTSU, 255, opencv_imgproc.THRESH_BINARY_INV + opencv_imgproc.THRESH_OTSU);

		Mat matTransform = Mat.eye(2, 3, opencv_core.CV_32F).asMat();
		// Initialize using existing transform
//		affine.setToTransform(mxx, mxy, tx, myx, myy, ty);
		try (FloatIndexer indexer = matTransform.createIndexer()) {
			indexer.put(0, 0, (float)affine.getMxx());
			indexer.put(0, 1, (float)affine.getMxy());
			indexer.put(0, 2, (float)(affine.getTx() / downsample));
			indexer.put(1, 0, (float)affine.getMyx());
			indexer.put(1, 1, (float)affine.getMyy());
			indexer.put(1, 2, (float)(affine.getTy() / downsample));
//			System.err.println(indexer);
		} catch (Exception e) {
			logger.error("Error closing indexer", e);
		}
//		// Might want to mask out completely black pixels (could indicate missing data)?
//		def matMask = new opencv_core.Mat(matOverlay.size(), opencv_core.CV_8UC1, Scalar.ZERO);
		TermCriteria termCrit = new TermCriteria(TermCriteria.COUNT, 100, 0.0001);
//		OpenCVTools.matToImagePlus(matBase, "Base").show();
//		OpenCVTools.matToImagePlus(matOverlay, "Overlay").show();
////		
//		Mat matTemp = new Mat();
//		opencv_imgproc.warpAffine(matOverlay, matTemp, matTransform, matBase.size());
//		OpenCVTools.matToImagePlus(matTemp, "Transformed").show();
		try {
			int motion;
			switch (registrationType) {
			case AFFINE:
				motion = opencv_video.MOTION_AFFINE;
				break;
			case RIGID:
				motion = opencv_video.MOTION_EUCLIDEAN;
				break;
			default:
				logger.warn("Unknown registration type {} - will use {}", registrationType, RegistrationType.AFFINE);
				motion = opencv_video.MOTION_AFFINE;
				break;
			}
			double result = opencv_video.findTransformECC(matBase, matOverlay, matTransform, motion, termCrit, null);
			logger.info("Transformation result: {}", result);
		} catch (Exception e) {
			Dialogs.showErrorNotification("Estimate transform", "Unable to estimate transform - result did not converge");
			logger.error("Unable to estimate transform", e);
			return;
		}
		
		// To use the following function, images need to be the same size
//		def matTransform = opencv_video.estimateRigidTransform(matBase, matOverlay, false);
		Indexer indexer = matTransform.createIndexer();
		affine.setToTransform(
			indexer.getDouble(0, 0),
			indexer.getDouble(0, 1),
			indexer.getDouble(0, 2) * downsample,
			indexer.getDouble(1, 0),
			indexer.getDouble(1, 1),
			indexer.getDouble(1, 2) * downsample
			);
		indexer.release();
		
//		matMask.release();
		matBase.release();
		matOverlay.release();
		matTransform.release();
	}
	
	/**
	 * Set the values of an Affine based on the contents of a 2x3 Mat.
	 * @param matTransform the transform data to use
	 * @param affine the Affine object to be updated
	 * @param downsample translation values will be scaled by the downsample. Relative scaling is otherwise assumed to be correct.
	 */
	static void matToAffine(Mat matTransform, Affine affine, double downsample) {
		Indexer indexer = matTransform.createIndexer();
		affine.setToTransform(
			indexer.getDouble(0, 0),
			indexer.getDouble(0, 1),
			indexer.getDouble(0, 2) * downsample,
			indexer.getDouble(1, 0),
			indexer.getDouble(1, 1),
			indexer.getDouble(1, 2) * downsample
			);
		indexer.release();
	}
	
	

	void requestShift(double dx, double dy) {
		ImageCombinerWarpyServerOverlay overlay = mapOverlays.get(selectedImageData.get());
		if (overlay == null) {
			Dialogs.showErrorNotification("Shift overlay", "No overlay selected!");
			return;
		}
		double downsample = Math.max(1.0, viewer.getDownsampleFactor());
		overlay.getAffine().appendTranslation(dx * downsample, dy * downsample);
	}

	void requestRotation(double theta) {
		ImageCombinerWarpyServerOverlay overlay = mapOverlays.get(selectedImageData.get());
		if (overlay == null) {
			Dialogs.showErrorNotification("Rotate overlay", "No overlay selected!");
			return;
		}
		overlay.getAffine().appendRotation(theta, viewer.getCenterPixelX(), viewer.getCenterPixelY());
	}
	
	//::dip
	void requestScaling(double scale) {
		ImageCombinerWarpyServerOverlay overlay = mapOverlays.get(selectedImageData.get());
		if (overlay == null) {
			Dialogs.showErrorNotification("Scale overlay", "No overlay selected!");
			return;
		}
		overlay.getAffine().appendScale(scale, scale);
	}
	
	
	static void requestShift(QuPathViewer viewer, Affine affine, double dx, double dy) {
		double downsample = Math.max(1.0, viewer.getDownsampleFactor());
		affine.appendTranslation(dx * downsample, dy * downsample);
	}

	static void requestRotation(QuPathViewer viewer, Affine affine, double theta) {
		affine.appendRotation(theta, viewer.getCenterPixelX(), viewer.getCenterPixelY());
	}
	
	//::dip
	static void requestScaling(QuPathViewer viewer, Affine affine, double scale) {
		affine.appendScale(scale, scale);
	}
	
	
	
	/**
	 * An event handler to enable interactively adjusting overlay transforms.
	 */
	class RefineTransformMouseHandler implements EventHandler<MouseEvent> {
		
		private Point2D pDragging;
		
		@Override
		public void handle(MouseEvent event) {
			if (!event.isPrimaryButtonDown() || event.isConsumed())
				return;
			
			ImageCombinerWarpyServerOverlay overlay = getSelectedOverlay();
			if (overlay == null)
				return;
				
			if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
				pDragging = viewer.componentPointToImagePoint(event.getX(), event.getY(), pDragging, true);
				return;
			} else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
				Point2D p = viewer.componentPointToImagePoint(event.getX(), event.getY(), null, true);
				if (event.isShiftDown() && pDragging != null) {
					double dx = p.getX() - pDragging.getX();
					double dy = p.getY() - pDragging.getY();
					overlay.getAffine().appendTranslation(-dx, -dy);
					event.consume();
				}
				pDragging = p;
			}
		}
	}
	
	
	
	/**
	 * ListCell for displaying image overlays.
	 */
	class ImageEntryCell extends ListCell<ImageData<BufferedImage>> {

		final SimpleDateFormat dateFormat = new SimpleDateFormat();
		
		private StackPane label = new StackPane();
		private Canvas viewCanvas = new Canvas();

		public ImageEntryCell() {
			double viewWidth = 80;
			double viewHeight = 60;
			viewCanvas.setWidth(viewWidth);
			viewCanvas.setHeight(viewHeight);
			viewCanvas.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 4, 0, 1, 1);");
			label.getChildren().add(viewCanvas);
			label.setPrefSize(viewWidth, viewHeight);
		}

		@Override
		protected void updateItem(ImageData<BufferedImage> item, boolean empty) {
			super.updateItem(item, empty);

			if (item == null || empty) {
				setText(null);
				setGraphic(null);
				return;
			}
			
			if (item == viewer.getImageData())
				setStyle("-fx-font-weight: bold; -fx-font-family: arial");
			else 
				setStyle("-fx-font-weight: normal; -fx-font-family: arial");
			
			// Get the name from the project, if possible
			Project<BufferedImage> project = qupath.getProject();
			String name = ServerTools.getDisplayableImageName(item.getServer());
			if (project != null) {
				ProjectImageEntry<BufferedImage> entry = project.getEntry(item);
				if (entry != null)
					name = entry.getImageName();
			}
			setText(name);
			
			BufferedImage img = viewer.getImageRegionStore().getThumbnail(item.getServer(), 0, 0, true);
			Image image = SwingFXUtils.toFXImage(img, null);
			GuiTools.paintImage(viewCanvas, image);
			if (getGraphic() == null)
				setGraphic(label);
				
		}				
	}
	
	
}