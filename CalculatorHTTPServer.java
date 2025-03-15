import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

public class CalculatorHTTPServer{

    public static void main(String[] args) throws IOException {

        int port = 8000;

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new CalculatorHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("Server is running on port " + port);

    }

    static class CalculatorHandler implements HttpHandler{

        private StringBuilder input = new StringBuilder();
        private boolean lastWasEvaluation = false;

        public void handle(HttpExchange exchange) throws IOException {

            try {

                String method = exchange.getRequestMethod();

                //Added this to handle HEAD request, without this, a HEAD request would be treated exactly like a GET request (response body included)
                if(method.equalsIgnoreCase("HEAD")){

                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, -1);

                    return;

                }

                String path = exchange.getRequestURI().getPath().substring(1);

                if(path.equals("favicon.ico")){

                    exchange.sendResponseHeaders(404, -1);
                    return;

                }
                
                if(!path.isEmpty()){

                    if(path.equals("=")){

                        String result = evaluateExpression(input.toString());
                        input.setLength(0);
                        input.append(result);

                        lastWasEvaluation = true;

                    } else if(path.equals("clear")){

                        input.setLength(0);

                        lastWasEvaluation = false;

                    } else{

                        if(lastWasEvaluation && isANumber(path)){

                            input.setLength(0);

                        }

                        input.append(path);

                        lastWasEvaluation = false;

                    }

                }

                String response = generateCalculatorPage(input.toString());
                byte[] responseBytes = response.getBytes("UTF-8");

                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();

            } catch(Exception e){

                System.err.println("Error handling request: " + e.getMessage());
                String errorResponse = "Internal Server Error";
                byte[] errorBytes = errorResponse.getBytes("UTF-8");
                exchange.sendResponseHeaders(500, errorBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(errorBytes);
                os.close();

            }

        }

        private boolean isANumber(String s){

            return s.matches("\\d");

        }


        private String generateCalculatorPage(String currentExpression) {

            StringBuilder html = new StringBuilder();
        
            html.append("<html>");
            html.append("<head>");
            html.append("<meta charset=\"UTF-8\">");
            html.append("<title>HTTP Calculator</title>");
            html.append("<style>");
            // Overall page styling
            html.append("body { ");
            html.append("  background-color: black; ");
            html.append("  color: #FFAC1C; ");
            html.append("  font-family: Arial, sans-serif; ");
            html.append("  display: flex; ");
            html.append("  flex-direction: column; ");
            html.append("  align-items: center; ");
            html.append("  justify-content: center; ");
            html.append("  height: 100vh; ");
            html.append("  margin: 0; ");
            html.append("} ");
            // Heading style
            html.append("h1 { ");
            html.append("  color: #FFAC1C; ");
            html.append("  margin-bottom: 20px; ");
            html.append("} ");
            // Calculator container styling
            html.append(".calculator { ");
            html.append("  background-color: #FFAC1C; ");
            html.append("  padding: 20px; ");
            html.append("  border-radius: 10px; ");
            html.append("  box-shadow: 0 4px 8px rgba(0,0,0,0.3); ");
            html.append("  text-align: center; ");
            html.append("} ");
            // Display area styling (showing only the current expression)
            html.append(".display { ");
            html.append("  background-color: black; ");
            html.append("  color: #FFAC1C; ");
            html.append("  padding: 10px; ");
            html.append("  margin-bottom: 20px; ");
            html.append("  font-size: 24px; ");
            html.append("  text-align: right; ");
            html.append("  border: 2px solid #FFAC1C; ");
            html.append("  border-radius: 5px; ");
            html.append("  width: 300px; ");
            html.append("  min-height: 40px; ");
            html.append("} ");
            // Grid container for buttons
            html.append(".buttons { ");
            html.append("  display: grid; ");
            html.append("  grid-template-columns: repeat(4, 1fr); ");
            html.append("  grid-gap: 10px; ");
            html.append("} ");
            // Styling for each button
            html.append(".buttons a { ");
            html.append("  display: block; ");
            html.append("  text-decoration: none; ");
            html.append("  background-color: black; ");
            html.append("  color: #FFAC1C; ");
            html.append("  font-size: 18px; ");
            html.append("  width: 100%; ");
            html.append("  height: 60px; ");
            html.append("  line-height: 60px; ");
            html.append("  text-align: center; ");
            html.append("  border-radius: 5px; ");
            html.append("} ");
            html.append(".buttons a:hover { ");
            html.append("  background-color: #333; ");
            html.append("} ");
            html.append("</style>");
            html.append("</head>");
            html.append("<body>");
            html.append("<div class=\"calculator\">");
            // New heading
            html.append("<h1>HTTP Calculator</h1>");
            // Display area without "Expression:" text
            html.append("<div class=\"display\">" + currentExpression + "</div>");
            html.append("<div class=\"buttons\">");
        
            List<String> calculatorButtons = List.of("1", "2", "3", "+", 
                                                       "4", "5", "6", "-", 
                                                       "7", "8", "9", "*", 
                                                       "0", "=", "/", "clear");
        
            for (String button : calculatorButtons) {
                if (button.equals("/")) { // Encode '/' correctly in URL
                    html.append("<a href=\"/%2F\">" + button + "</a>");
                } else {
                    html.append("<a href=\"/" + button + "\">" + button + "</a>");
                }
            }
        
            html.append("</div>"); // close buttons grid
            html.append("</div>"); // close calculator container
            html.append("</body></html>");
        
            return html.toString();
        }
        
        

        private String evaluateExpression(String expr){

            try{

                ExpressionParser parser = new ExpressionParser(expr);
                double result = parser.parse();
                return Double.toString(result);

            } catch(Exception e){

                e.printStackTrace();
                return "Error";

            }

        }
        
        private static class ExpressionParser{

            private final String input;
            private int position = -1;
            private int ch;
        
            public ExpressionParser(String input){

                this.input = input;
                nextChar();

            }
        
            //move on one character
            private void nextChar(){

                position++;

                if(position < input.length()){

                    ch = input.charAt(position);

                } else{

                    ch = -1;

                }

            }
        
            //accept character if it matches the particular character looked for
            private boolean accept(int charToEat){

                while(ch == ' '){

                    nextChar();

                } 

                if(ch == charToEat){

                    nextChar();
                    return true;

                }

                return false;

            }
        
            //parse entire expressin
            public double parse(){

                double x = parseExpression();

                if(position < input.length()) throw new RuntimeException("Unexpected: " + (char)ch);

                return x;

            }
        
            //define grammer:
            //Handle + or - operators
            private double parseExpression(){

                double x = parseTerm();

                //create endless loop
                for(;;){

                    if(accept('+')){

                        x += parseTerm();

                    } else if(accept('-')){

                        x -= parseTerm();

                    } else{

                        return x;

                    }

                }
            }
        
            //define * and / operators
            private double parseTerm(){

                double x = parseFactor();

                //create endless loop
                for(;;){

                    if(accept('*')){

                        x *= parseFactor();
                        
                    } else if(accept('/')){

                        x /= parseFactor();

                    } else{

                        return x;

                    }

                }
            }
        
            //handle + - operators
            private double parseFactor(){

                if (accept('+')) return parseFactor();
                if (accept('-')) return -parseFactor();
        
                double x;
                int startPos = this.position;

                if(accept('(')){

                    x = parseExpression();
                    if(!accept(')')) throw new RuntimeException("Missing ')'");

                } else if((ch >= '0' && ch <= '9') || ch == '.'){

                    while ((ch >= '0' && ch <= '9') || ch == '.'){

                        nextChar();

                    } 

                    String numberStr = input.substring(startPos, position);
                    x = Double.parseDouble(numberStr);

                } else{

                    throw new RuntimeException("Unexpected character: " + (char)ch);

                }

                return x;

            }
            
        }

    }

}
