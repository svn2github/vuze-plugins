/**
 * 
 */
package com.aelitis.azureus.plugins.yeenyeen;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

/**
 * This creates the scheduler-like widget in the composite given. Also
 * includes the label.
 */
class SchedulerWidget {
	
	private Image[] colour_array;
	private Image[] selected_colour_array;
	private Image[] grey_colour_array;
	private int[][] rgb_modes;
	private boolean enabled;
	private Runnable repainter;
	
	private SchedulerController controller;
	private MouseStateListener[] mouse_state_holder;
	
	public SchedulerWidget(SchedulerController controller, int[][] rgb_modes) {
		this.controller = controller;
		this.rgb_modes = rgb_modes;
		this.colour_array = new Image[this.rgb_modes.length];
		this.selected_colour_array = new Image[this.rgb_modes.length];
		this.grey_colour_array = new Image[this.rgb_modes.length];
		this.mouse_state_holder = new MouseStateListener[1];
		this.enabled = true;
	}		
	
	public void prepare(Composite parent) { 
		parent.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				SchedulerWidget.this.dispose();
			}
		});
		
		Display display = parent.getDisplay();
		for (int i=0; i<this.colour_array.length; i++) {
			this.colour_array[i] = makeImage(display, this.rgb_modes[i]);
			this.selected_colour_array[i] = makeDarkerImage(display, this.rgb_modes[i]);
			this.grey_colour_array[i] = makeGreyImage(display, this.rgb_modes[i]);
		}
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (this.repainter != null) {
			this.repainter.run();
		}
	}
	
	public void dispose() {
		for (int i=0; i<this.colour_array.length; i++) {
			if (!colour_array[i].isDisposed()) {colour_array[i].dispose();}
			if (!selected_colour_array[i].isDisposed()) {selected_colour_array[i].dispose();}
			if (!grey_colour_array[i].isDisposed()) {selected_colour_array[i].dispose();}
		}
	}

	public void createScheduler(Composite container, int[][] initial_states) {
		final int BOX_WIDTH = this.colour_array[0].getImageData().width;
		final int BOX_HEIGHT = this.colour_array[0].getImageData().height;
		
		RowData data = new RowData();
		final RowLayout layout = new RowLayout();
		layout.type = SWT.VERTICAL;
		layout.spacing = 1;
		layout.marginTop = 5;
		layout.marginBottom = 5;
		container.setLayout(layout);
		container.setLayoutData(data);
		
		// Defines the default behaviour.
		this.mouse_state_holder[0] = new DefaultMouseStateListener();
		
		// This listener is just used for normal exit events, we need
		// it because it's more difficult to determine otherwise.
		MouseTrackAdapter exit_listener = new MouseTrackAdapter() {
			public void mouseExit(MouseEvent e) {
				mouse_state_holder[0].onExit((Block)e.widget.getData("block"));
			}
		};
		
		// This is where we detect mouse clicks.
		MouseListener click_listener = new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				boolean controlled = e.button == 3 || (e.button == 1 && ((e.stateMask & SWT.CONTROL) == SWT.CONTROL));
				Block block = (Block)e.widget.getData("block");
				mouse_state_holder[0].onClick(block, controlled);
			}
			public void mouseUp(MouseEvent e) {
				boolean controlled = e.button == 3 || (e.button == 1 && ((e.stateMask & SWT.CONTROL) == SWT.CONTROL));
				Block block = (Block)e.widget.getData("block");
				mouse_state_holder[0].onClickRelease(block, controlled);
			}
		};
		
		final RowLayout r_layout = new RowLayout();
		r_layout.spacing = 1;
		r_layout.marginTop = 0;
		r_layout.marginBottom = 0;
		r_layout.marginLeft = 5;
		r_layout.marginRight = 5;

		// We need these defined to determine where we are.
		final int MARGIN_WIDTH = r_layout.spacing;
		final int MARGIN_AND_BOX_WIDTH = MARGIN_WIDTH + BOX_WIDTH;
		final int MARGIN_HEIGHT = r_layout.marginTop + r_layout.marginBottom + layout.spacing;
		final int MARGIN_AND_BOX_HEIGHT = MARGIN_HEIGHT + BOX_HEIGHT;

		// This mouse listener provides us with the mouse-enter and mouse-exit
		// events which we lack when the mouse button is depressed - it works
		// based on the x/y co-ordinate offsets based on the original block
		// that was selected.
		final Block[][] blocks = new Block[24][7];
		MouseMoveListener move_listener = new MouseMoveListener() {
			public void mouseMove(MouseEvent e) {
				Block block = ((Block)e.widget.getData("block"));
				if (0 <= e.x && e.x < BOX_WIDTH && 0 <= e.y && e.y < BOX_HEIGHT) {
					mouse_state_holder[0].onEnter(block);
					return;
				}

				// e.x and e.y are relative to the top corner of the box.
				//
				// The following calculations are done so that the behaviour is
				// the same if the box is either on the left side or above us.
				boolean neg_x = e.x < 0;
				boolean neg_y = e.y < 0;
				int e_x = (neg_x) ? (-e.x) + BOX_WIDTH : e.x;
				int e_y = (neg_y) ? (-e.y) + BOX_HEIGHT : e.y;

				// This is how many boxes we've definitely skipped.
				int box__x = e_x / MARGIN_AND_BOX_WIDTH;
				int box__y = e_y / MARGIN_AND_BOX_HEIGHT;

				// Are we in a box?
				if ((e_x % MARGIN_AND_BOX_WIDTH) < BOX_WIDTH ) {}
				else {
					mouse_state_holder[0].onExitGrid();
					return;
				}

				// Are we in a box?
				if ((e_y % MARGIN_AND_BOX_HEIGHT) < BOX_HEIGHT ) {}
				else {
					mouse_state_holder[0].onExitGrid();
					return;
				}

				// Fix up negativity!
				if (neg_x) {box__x = -box__x;}
				if (neg_y) {box__y = -box__y;}

				// Well, we appear to be in a box, but let us just bounds check first.
				// We'll reuse these values from being relative to absolute.
				box__x += block.x;
				box__y += block.y;

				if (0 <= box__x && box__x < 24) {}
				else {
					mouse_state_holder[0].onExitGrid();
					return;
				}

				if (0 <= box__y && box__y < 7) {}
				else {
					mouse_state_holder[0].onExitGrid();
					return;
				}

				mouse_state_holder[0].onEnter(blocks[box__x][box__y]);
			}
		};
		
		for (int i=0; i<7; i++) {
			Composite mc = new Composite(container, SWT.NONE);

			RowData r_data = new RowData();
			mc.setLayout(r_layout);
			mc.setLayoutData(r_data);

			for (int j=0; j<24; j++) {
				final Label label = new Label(mc, SWT.NONE);
				Block block = new Block(j, i, label, initial_states[i][j]);
				blocks[j][i] = block;
				label.setData("block", block);
				block.repaint();

				label.addMouseListener(click_listener);
				label.addMouseTrackListener(exit_listener);
				label.addMouseMoveListener(move_listener);
			}
			mc.layout();
			mc.pack();
		}
		
		this.repainter = new Runnable() {
			public void run() {
				for (int i=0; i<blocks.length; i++) {
					for (int j=0; j<blocks[i].length; j++) {
						blocks[i][j].repaint();
					}
				}
			}
		};

		Composite label_thing = new Composite(container, SWT.NONE);
		label_thing.setLayout(r_layout);
		label_thing.setLayoutData(new RowData());
		
		Label identifying_label = new Label(label_thing, SWT.NONE);
		identifying_label.setData("container", container);
		mouse_state_holder[0].setLabel(identifying_label);
		
		container.pack();
		container.layout();

	}
	
	public void repaint() {
		this.repainter.run();
	}
	
	public void createLegend(Composite container, String[] states) {
		GridLayout grid_layout = new GridLayout();
		grid_layout.marginLeft = 10;
		grid_layout.marginRight = 10;
		grid_layout.horizontalSpacing = 50;
		grid_layout.numColumns = (states.length <= 4) ? 2 : 3;
		container.setLayout(grid_layout);
		container.setLayoutData(new GridData());

		for (int i=0; i<colour_array.length; i++) {
			Composite label_part = new Composite(container, SWT.NONE);
			label_part.setLayout(new RowLayout());
			Label lbl = new Label(label_part, SWT.LEFT);
			lbl.setImage(colour_array[i]);
			lbl = new Label(label_part, SWT.LEFT);
			lbl.setText(states[i]);
			label_part.pack();
			label_part.layout();
		}
		
		container.pack();
		container.layout();
	}
	
	Image getImage(int colour_index, boolean selected) {
		Image[] image_array = selected ? this.selected_colour_array : this.colour_array;
		if (!enabled) {image_array = this.grey_colour_array;}
		return image_array[colour_index];
	}
	        
    private class Block {
    	int x, y, colour;
    	boolean selected;
    	Label label;
    	private Image chosen_image;
    	
    	public Block(int x, int y, Label label, int colour) {
    		this.x = x;
    		this.y = y;
    		this.label = label;
    		this.colour = colour;
    	}
    	
 	   public void setSelected(boolean selected) {
		   this.selected = selected;
		   repaint();
	   }
 	   
 	   public void setColour(int colour) {
 		   this.colour = colour;
 		   repaint();
 	   }
 	   
 	   public int nextState() {
		   this.colour++;
		   if (this.colour == colour_array.length) {this.colour = 0;}
		   repaint();
		   return this.colour;
	   }
 	   
 	   public Image getImage() {
  		  if (this.chosen_image != null) {return this.chosen_image;}
  		  else {return SchedulerWidget.this.getImage(this.colour, this.selected);}
  	   }
  	   
 	   private void repaint() {
 		   label.setImage(getImage());
 	   }
    	
    }
    
    public interface MouseStateListener {
    	public void onClick(Block block, boolean controlled);
    	public void onClickRelease(Block block, boolean controlled);
    	public void onEnter(Block block);
    	public void onExit(Block block);
    	public void onExitGrid();
    	public void setLabel(Label label);
    }
        
    public class DefaultMouseStateListener implements MouseStateListener {
    	
    	private Block selected_block = null;
    	private Integer use_colour = null;
    	private Label label = null;
    	
    	private boolean is_controlled = false; 
    	
    	public void onClick(Block block, boolean controlled) {
    		if (!enabled) {return;}
    		if (controlled) {
    			this.is_controlled = true;
    			return;
    		}
    		this.use_colour = Integer.valueOf(block.nextState());
    	}
    	
    	public void onClickRelease(Block block, boolean controlled) {
    		if (controlled) {
    			if (!this.is_controlled) {return;}
    			this.is_controlled = false;
    			controller.modifyBlock(block.y, block.x);
    		}
    		this.use_colour = null;
    	}
    	
    	public void onEnter(Block block) {
    		if (this.selected_block != null && this.selected_block != block) {
    			onExit(this.selected_block);
    		}
    		
    		block.setSelected(true);
    		
    		if (this.use_colour != null) {
    			block.setColour(this.use_colour.intValue());
    		}
    		
    		this.label.setText(controller.describeBlock(block.y, block.x));
    		((Composite)this.label.getData("container")).layout();
    		this.selected_block = block;
    	}
    	
    	public void onExit(Block block) {
    		this.is_controlled = false;
    		block.setSelected(false);
    		if (this.selected_block == block) {
    			this.selected_block = null;
    		}
    	}
    	
    	public void onExitGrid() {
    		if (this.selected_block != null) {
    			onExit(this.selected_block);
    		}
    	}
    	
    	public void setLabel(Label label) {
    		this.label = label;
    	}
    	
    }

    private static Image makeImage(Display d, int[] rgb) {
    	RGB[] rgbs = new RGB[] {
    		new RGB(rgb[0], rgb[1], rgb[2]), new RGB(0, 0, 0)
    	};
    	PaletteData palette = new PaletteData(rgbs);
    	ImageData image = new ImageData(17, 17, 1, palette);
    	int [] ends = new int[] {0, 16};
    	for (int i=0; i<ends.length; i++) {
    		for (int j=0; j<17; j++) {
    			image.setPixel(ends[i], j, 1);
    		}
    	}
    	for (int i=0; i<ends.length; i++) {
    		for (int j=0; j<17; j++) {
    			image.setPixel(j, ends[i], 1);
    		}
    	}
    	return new Image(d, image);
    }
    
    private static Image makeDarkerImage(Display d, int[] rgb) {
    	return makeImage(d, new int[] {Math.max(0, rgb[0]-22), Math.max(0, rgb[1]-22), Math.max(0, rgb[2]-22)});
    }
    
    private static Image makeGreyImage(Display d, int[] rgb) {
    	int y = (int)((0.3 * rgb[0]) + (0.59 * rgb[1]) + (0.11 * rgb[2]));
    	return makeImage(d, new int[] {y, y, y});
    }


}
