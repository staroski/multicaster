package br.com.staroski.multicaster;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

public class MulticasterGeneratorUI extends JFrame {

    private static final long serialVersionUID = 1;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            JFrame frame = new MulticasterGeneratorUI();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private JTextField textFieldClassName;
    private JTextField textFieldListenerName;
    private JTextArea textAreaGenerated;

    private MulticasterGeneratorUI() {
        super("Multicaster Generator");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(new Dimension(800, 600));
        setMinimumSize(new Dimension(640, 480));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (confirm("Do you really want to exit?")) {
                    System.exit(0);
                }
            }
        });
        Container container = getContentPane();
        container.add(createNorthPanel(), BorderLayout.NORTH);
        container.add(createCenterPanel(), BorderLayout.CENTER);
        container.add(createSouthPanel(), BorderLayout.SOUTH);
    }

    private boolean confirm(String message) {
        int option = JOptionPane.showConfirmDialog(this, message, "Question", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return option == JOptionPane.YES_OPTION;
    }

    private Component createCenterPanel() {
        textAreaGenerated = new JTextArea();
        TextLineNumber textLineNumber = new TextLineNumber(textAreaGenerated);
        JScrollPane scrollPane = new JScrollPane(textAreaGenerated);
        scrollPane.setRowHeaderView(textLineNumber);
        try {
            Class<? extends MulticasterGeneratorUI> type = getClass();
            String fontPath = "/" + type.getPackage().getName().replace('.', '/') + "/cour.ttf";
            InputStream input = type.getResourceAsStream(fontPath);
            Font font = Font.createFont(Font.TRUETYPE_FONT, input).deriveFont(Font.PLAIN, 14);
            textAreaGenerated.setFont(font);
            textLineNumber.setFont(font);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private Component createNorthPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JLabel labelClassName = new JLabel("Generated class name:");
        labelClassName.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(labelClassName);
        textFieldClassName = new JTextField();
        textFieldClassName.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(textFieldClassName);
        JLabel listenerName = new JLabel("Interface to implement: (Use comma to separate more than one)");
        listenerName.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(listenerName);
        textFieldListenerName = new JTextField();
        textFieldListenerName.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(textFieldListenerName);
        return panel;
    }

    private Component createSouthPanel() {
        JButton generate = new JButton("Generate");
        generate.addActionListener(event -> generate());
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.add(generate);
        return panel;
    }

    private void error(Throwable error) {
        StringBuilder text = new StringBuilder(error.getClass().getSimpleName());
        String message = error.getMessage();
        if (message != null) {
            text.append(":\n").append(message).append("");
        }
        JOptionPane.showMessageDialog(this, text.toString(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void generate() {
        String className = textFieldClassName.getText();
        if (className == null || (className = className.trim()).isEmpty()) {
            warn("Generated class name is mandatory!");
            return;
        }
        String listenerNames = textFieldListenerName.getText();
        if (listenerNames == null || (listenerNames = listenerNames.trim()).isEmpty()) {
            warn("Listener interface name is mandatory!");
            return;
        }
        try {
            MulticasterGenerator generator = new MulticasterGenerator();
            String generatedCode = generator.generate(className, listenerNames.split("\\,"));
            textAreaGenerated.setText(generatedCode);
            textAreaGenerated.setCaretPosition(0);
            textAreaGenerated.requestFocus();
        } catch (Exception e) {
            e.printStackTrace();
            error(e);
        }
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }
}
