import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.List;

public class CalculatorHTTPServer {

    public static void main(String[] args) throws IOException {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new CalculatorHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server is running on port " + port);
    }

    static class CalculatorHandler implements HttpHandler {
        private StringBuilder input = new StringBuilder();
        private boolean lastWasEvaluation = false;

        public void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod();

                // Construct the current date in the correct HTTP format
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                String dateString = dateFormat.format(new Date());

                // Set up the HTTP response headers to be fully compliant
                Headers responseHeaders = exchange.getResponseHeaders();
                responseHeaders.set("Date", dateString);
                responseHeaders.set("Server", "CalculatorHTTPServer/1.0");
                responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
                responseHeaders.set("Connection", "close");

                // Handle HEAD requests: send headers only with no response body
                if (method.equalsIgnoreCase("HEAD")) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }

                String path = exchange.getRequestURI().getPath().substring(1);

                if (path.equals("favicon.ico")) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }

                if (!path.isEmpty()) {
                    if (path.equals("=")) {
                        String result = evaluateExpression(input.toString());
                        input.setLength(0);
                        input.append(result);
                        lastWasEvaluation = true;
                    } else if (path.equals("clear")) {
                        input.setLength(0);
                        lastWasEvaluation = false;
                    } else {
                        if (lastWasEvaluation && isANumber(path)) {
                            input.setLength(0);
                        }
                        input.append(path);
                        lastWasEvaluation = false;
                    }
                }

                String body = generateCalculatorPage(input.toString());
                byte[] responseBytes = body.getBytes("UTF-8");

                // The sendResponseHeaders call here sends a status line ("HTTP/1.1 200 OK")
                // along with the headers we have manually set.
                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();
            } catch (Exception e) {
                System.err.println("Error handling request: " + e.getMessage());
                String errorResponse = "Internal Server Error";
                byte[] errorBytes = errorResponse.getBytes("UTF-8");
                exchange.sendResponseHeaders(500, errorBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(errorBytes);
                os.close();
            }
        }

        private boolean isANumber(String s) {
            return s.matches("\\d");
        }

        private String generateCalculatorPage(String currentExpression) {
            StringBuilder html = new StringBuilder();
            html.append("<html>");
            html.append("<head>");
            html.append("<meta charset=\"UTF-8\">");
            html.append("<title>HTTP Calculator</title>");
            html.append("<style>");
            html.append("body { background-color: black; color: #FFAC1C; font-family: Arial, sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; margin: 0; }");
            html.append("h1 { color: #FFAC1C; margin-bottom: 20px; }");
            html.append(".calculator { background-color: #FFAC1C; padding: 20px; border-radius: 10px; box-shadow: 0 4px 8px rgba(0,0,0,0.3); text-align: center; }");
            html.append(".display { background-color: black; color: #FFAC1C; padding: 10px; margin-bottom: 20px; font-size: 24px; text-align: right; border: 2px solid #FFAC1C; border-radius: 5px; width: 300px; min-height: 40px; }");
            html.append(".buttons { display: grid; grid-template-columns: repeat(4, 1fr); grid-gap: 10px; }");
            html.append(".buttons a { display: block; text-decoration: none; background-color: black; color: #FFAC1C; font-size: 18px; width: 100%; height: 60px; line-height: 60px; text-align: center; border-radius: 5px; }");
            html.append(".buttons a:hover { background-color: #333; }");
            html.append("</style>");
            html.append("</head>");
            html.append("<body>");
            html.append("<div class=\"calculator\">");
            html.append("<h1>HTTP Calculator</h1>");
            html.append("<div class=\"display\">" + currentExpression + "</div>");
            html.append("<div class=\"buttons\">");

            List<String> calculatorButtons = List.of("1", "2", "3", "+", 
                                                     "4", "5", "6", "-", 
                                                     "7", "8", "9", "*", 
                                                     "0", "=", "/", "clear");

            for (String button : calculatorButtons) {
                // Ensure the '/' is correctly URL-encoded
                if (button.equals("/")) {
                    html.append("<a href=\"/%2F\">" + button + "</a>");
                } else {
                    html.append("<a href=\"/" + button + "\">" + button + "</a>");
                }
            }

            html.append("</div>"); // Close buttons grid
            html.append("</div>"); // Close calculator container
            html.append("</body></html>");

            return html.toString();
        }

        private String evaluateExpression(String expr) {
            try {
                ExpressionParser parser = new ExpressionParser(expr);
                double result = parser.parse();
                return Double.toString(result);
            } catch (Exception e) {
                e.printStackTrace();
                return "Error";
            }
        }

        // A simple recursive descent parser to evaluate arithmetic expressions
        private static class ExpressionParser {
            private final String input;
            private int position = -1;
            private int ch;

            public ExpressionParser(String input) {
                this.input = input;
                nextChar();
            }

            private void nextChar() {
                position++;
                if (position < input.length()) {
                    ch = input.charAt(position);
                } else {
                    ch = -1;
                }
            }

            private boolean accept(int charToEat) {
                while (ch == ' ') {
                    nextChar();
                }
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            public double parse() {
                double x = parseExpression();
                if (position < input.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            private double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if (accept('+')) {
                        x += parseTerm();
                    } else if (accept('-')) {
                        x -= parseTerm();
                    } else {
                        return x;
                    }
                }
            }

            private double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (accept('*')) {
                        x *= parseFactor();
                    } else if (accept('/')) {
                        x /= parseFactor();
                    } else {
                        return x;
                    }
                }
            }

            private double parseFactor() {
                if (accept('+')) return parseFactor();
                if (accept('-')) return -parseFactor();
                double x;
                int startPos = this.position;
                if (accept('(')) {
                    x = parseExpression();
                    if (!accept(')')) throw new RuntimeException("Missing ')'");
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') {
                        nextChar();
                    }
                    String numberStr = input.substring(startPos, position);
                    x = Double.parseDouble(numberStr);
                } else {
                    throw new RuntimeException("Unexpected character: " + (char)ch);
                }
                return x;
            }
        }
    }
}
