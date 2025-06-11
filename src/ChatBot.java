import javax.swing.*;
import org.json.JSONObject; // You will need to add the org.json library
import org.json.JSONArray;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ChatBot {
    public static void main(String[] args) {
        JFrame frame = new JFrame("ChatBot");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 600);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        // Make text wrap
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());

        JTextField inputField = new JTextField();
        JButton sendButton = new JButton("Send");

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(inputPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);

        // Allow sending by pressing Enter in the input field
        inputField.addActionListener(e -> sendButton.doClick());

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String userInput = inputField.getText();
                if (!userInput.isEmpty()) {
                    chatArea.append("You: " + userInput + "\n\n");
                    inputField.setText("");

                    // Show a "thinking" message
                    chatArea.append("ChatBot: Thinking...\n\n");

                    // Asynchronous API call
                    new Thread(() -> {
                        String botResponse = getChatBotResponse(userInput);
                        SwingUtilities.invokeLater(() -> {
                            // Remove "Thinking..." message and append the actual response
                            chatArea.setText(chatArea.getText().replace("ChatBot: Thinking...\n\n", ""));
                            chatArea.append("ChatBot: " + botResponse + "\n\n");
                        });
                    }).start();
                }
            }
        });

        frame.add(panel);
        frame.setVisible(true);
    }

    private static String getChatBotResponse(String input) {
        try {
            // Correct API endpoint for chat completions
            URL url = new URL("https://openrouter.ai/api/v1/chat/completions");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");

            // ** IMPORTANT: Replace with your actual API key **
            con.setRequestProperty("Authorization", "Bearer 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");

            // Recommended header to identify your application
            con.setRequestProperty("HTTP-Referer", "http://localhost"); // Replace with your website or app name

            con.setDoOutput(true);

            // Correct JSON payload structure
            String jsonInputString = new JSONObject()
                    .put("model", "mistralai/mistral-7b-instruct") // Specify a valid model here
                    .put("messages", new JSONArray()
                            .put(new JSONObject()
                                    .put("role", "user")
                                    .put("content", input)))
                    .toString();

            try (OutputStream os = con.getOutputStream()) {
                byte[] requestBody = jsonInputString.getBytes("utf-8");
                os.write(requestBody, 0, requestBody.length);
            }

            // Check if the request was successful (HTTP 200)
            int responseCode = con.getResponseCode();
            InputStream inputStream;
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = con.getInputStream();
            } else {
                // If not successful, read the error stream to see what went wrong
                inputStream = con.getErrorStream();
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return "Error from API: " + response.toString();
                }

                // Parse the JSON response to get the content
                JSONObject jsonResponse = new JSONObject(response.toString());
                return jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            return "Error: An exception occurred. Check the console for details.";
        }
    }
}