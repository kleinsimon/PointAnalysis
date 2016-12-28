import java.awt.Button;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ij.ImagePlus;
import ij.gui.YesNoCancelDialog;
import ij.measure.ResultsTable;

public class PointAnalysisInteractiveMenuStrip extends Panel implements ActionListener, ItemListener {
	private static final long serialVersionUID = 1L;
	private Label infoLabel, countLabel;
	private Button okButton, cancelButton, clearButton;
	private java.awt.Choice colorSelect;
	public PointAnalysisInteractiveHandler interactionHandler;
	private String[] colorList = {"Red","Green","Blue","Yellow","Orange","Purple","Black","White"};

	public PointAnalysisInteractiveMenuStrip(int pointsX, int pointsY, String overlayColor, int numDomains, Boolean randomizePoints, 
			int markLenPx, ImagePlus image, ResultsTable restable) {

		//infoLabel = new Label();
		//infoLabel.setText("Left: Toggle. Right: Reverse Toggle.");

		countLabel = new Label();
		countLabel.setText("");
		
		colorSelect = new java.awt.Choice();
		for (String c : colorList)
			colorSelect.add(c);
		colorSelect.select(overlayColor);
		colorSelect.addItemListener(this);

		okButton = new Button();
		okButton.setLabel("OK");
		okButton.setActionCommand("OK");
		okButton.addActionListener(this);

		cancelButton = new Button();
		cancelButton.setLabel("Cancel");
		cancelButton.setActionCommand("Cancel");
		cancelButton.addActionListener(this);

		clearButton = new Button();
		clearButton.setLabel("Clear");
		clearButton.setActionCommand("Clear");
		clearButton.addActionListener(this);

		//this.add(infoLabel);
		this.add(countLabel);
		this.add(colorSelect);
		this.add(okButton);
		this.add(cancelButton);
		this.add(clearButton);
		
		interactionHandler = new PointAnalysisInteractiveHandler(pointsX, pointsY, overlayColor, numDomains, randomizePoints,
				markLenPx, image, restable, this);
	}

	public void actionPerformed(ActionEvent e) {
		String s = e.getActionCommand();
		if (s == "OK") {
			interactionHandler.analyze();
		} else if (s == "Cancel") {
			remove();
		} else if (s == "Clear") {
			clear();
		}
	}

	public void updateCounter(Integer[] count) {
		String s = "";
		for (Integer c : count){
			if (!s.isEmpty())
				s+="|";
			s+=c.toString();
		}
		countLabel.setText(s);
	}

	private void clear() {
		if (confirm("Really clear marks?"))
			interactionHandler.clear();
	}

	public boolean remove() {
		if (confirm("Cancel measurement? This will reset all marks.")) {
			interactionHandler.remove();
			this.getParent().remove(this);
			return true;
		}
		return false;
	}

	public Boolean confirm(String Message) {
		YesNoCancelDialog dg = new YesNoCancelDialog(null, "Confirm", Message);
		if (dg.yesPressed())
			return true;
		else
			return false;
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() != ItemEvent.SELECTED)
			return;
		
		interactionHandler.setColor((String) e.getItem());
		interactionHandler.drawOverlay();
	}
}
