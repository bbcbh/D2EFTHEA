package gui;

import java.awt.EventQueue;
import java.util.Hashtable;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import util.D2EFT_QALY_SF12;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.JSlider;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.awt.event.ActionEvent;

public class D2EFT_SF12_Form extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8661014398408726330L;
	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					D2EFT_SF12_Form frame = new D2EFT_SF12_Form();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public D2EFT_SF12_Form() {

		Hashtable<Integer, JLabel> ent;
		JSlider[] slider_collection = new JSlider[D2EFT_QALY_SF12.SF12_LENGTH];

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 900, 950);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));

		JLabel lblSfTestForm = new JLabel("SF-12 Test Form for D2EFT");
		lblSfTestForm.setFont(new Font("FreeSans", Font.BOLD, 20));
		lblSfTestForm.setHorizontalAlignment(SwingConstants.CENTER);
		contentPane.add(lblSfTestForm);

		JPanel questionPanel = new JPanel();
		contentPane.add(questionPanel);
		questionPanel.setLayout(new BoxLayout(questionPanel, BoxLayout.PAGE_AXIS));

		JPanel buttonPanel = new JPanel();
		contentPane.add(buttonPanel);
		
		JPanel outputPanel = new JPanel();
		contentPane.add(outputPanel);

		JLabel lblOutput = new JLabel("");
		outputPanel.add(lblOutput);

		for (int i = 0; i < slider_collection.length; i++) {
			slider_collection[i] = new JSlider();
			slider_collection[i].setMinimum(1);
			slider_collection[i].setMaximum(D2EFT_QALY_SF12.SF12_Options[i].length);
			slider_collection[i].setValue(D2EFT_QALY_SF12.SF12_Options_Default[i]);
			slider_collection[i].setMajorTickSpacing(1);
			slider_collection[i].setSnapToTicks(true);
			slider_collection[i].setPaintLabels(true);
			slider_collection[i].setPaintTicks(true);
			ent = new Hashtable<Integer, JLabel>();
			for (int j = 0; j < D2EFT_QALY_SF12.SF12_Options[i].length; j++) {
				ent.put(j + 1, new JLabel(D2EFT_QALY_SF12.SF12_Options[i][j]));
			}
			slider_collection[i].setLabelTable(ent);
			slider_collection[i].setBorder(BorderFactory.createTitledBorder(D2EFT_QALY_SF12.SF12_QUESTIONS_TEXT[i]));
			questionPanel.add(slider_collection[i]);
			questionPanel.add(Box.createVerticalStrut(5));
		}
		

		JButton btnCalculate = new JButton("Calculate");
		buttonPanel.add(btnCalculate);
		
		btnCalculate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int[] ans = new int[slider_collection.length];
				for (int i = 0; i < ans.length; i++) {
					ans[i] = slider_collection[i].getValue();
				}				
				try {
					float[] scale = D2EFT_QALY_SF12.calulateSummaryScale(ans);
					String output = String.format("PCS-12 (Physical Score) = %.5f, MCS-12 (Mental Score) = %.5f", scale[0], scale[1]);					
					lblOutput.setText(output);
					
				} catch (IllegalArgumentException ex) {						
					PrintWriter str = new PrintWriter(new StringWriter());		
					ex.printStackTrace(str);
					JOptionPane.showMessageDialog(btnCalculate.getRootPane(), str.toString(), ex.toString(), JOptionPane.ERROR_MESSAGE);

				}

			}
		});
		

		

	}

}
