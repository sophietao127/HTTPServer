import java.io.*;
import java.net.*;

public class HttpServer {
    // A method for create a response to GET request
    public static String handleGetRequest(String[] getFile, File findingFile, String CRLF) throws Exception {
        String contentType = "";
        if(getFile[1].contains("html")) {
            contentType = "text/html";
        }
        else if(getFile[1].contains("txt")) {
            contentType = "text/plain";
        }

        int contentLength = (int) findingFile.length();

        StringBuilder fileContent = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(getFile[1].substring(1)));
        String emptyString = "";
        while ((emptyString = reader.readLine()) != null) {
            fileContent.append(emptyString);
        }
        reader.close();

        String response = "HTTP/1.1 200 OK" + CRLF + "Content-Type: " + contentType + CRLF + "Content-Length: " + Integer.toString(contentLength) + CRLF + CRLF + fileContent.toString();
        return response;
    }

    // A method for create a response to POST request
    public static String handlePostRequest(String[] getFile, InputStream in, String CRLF) throws Exception {
        //get the content of the file.
        StringBuilder fileContent = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(getFile[1].substring(1)));
        String emptyString = "";
        while ((emptyString = reader.readLine()) != null) {
            fileContent.append(emptyString);
        }
        reader.close();

        //Find the header information
        String requestString = "";
        int character = 0;
        while(((character = in.read()) != -1) && (requestString.indexOf("\r\n\r\n") == -1)) {
            requestString = requestString + ((char) character);
        }
        String[] postRequest = requestString.split("\n");

        //Find the content length
        int contentLength = 0;
        String contentType = "";
        for(int i = 0; i < postRequest.length; i++) {
            if(postRequest[i].contains("Length")) {
                String[] contentLengthString = postRequest[i].split(" ");
                contentLength = Integer.parseInt(contentLengthString[1].trim());
            }
            else if(postRequest[i].contains("Type")) {
                String[] contentTypeString = postRequest[i].split(" ");
                contentType = contentTypeString[1].trim();
            }
        }

        //get the message from the body
        String message = "" + (char) character;
        int charc = 0;
        while(message.length() != contentLength) {
            charc = in.read();
            message = message + ((char) charc);
        }

        FileWriter writer = new FileWriter(getFile[1].substring(1));
        writer.write(fileContent.toString() + " " + message);
        writer.close();

        String response = "HTTP/1.1 200 OK" + CRLF + "Content-Type: " + contentType + "\r\n\r\n" + "Content-Length: " + Integer.toString(contentLength) + CRLF + CRLF + fileContent.toString() + " " + message;
        return response;
    }

    // A method for create a response to PUT request
    public static String handlePutRequest(String[] getFile, InputStream in, String CRLF) throws Exception {
        //Find the header information
        String requestString = "";
        int character = 0;
        while(((character = in.read()) != -1) && (requestString.indexOf("\r\n\r\n") == -1)) {
            requestString = requestString + ((char) character);
        }
        String[] postRequest = requestString.split("\n");
        System.out.println(requestString);

        //Find the content length
        int contentLength = 0;
        String contentType = "";
        for(int i = 0; i < postRequest.length; i++) {
            if(postRequest[i].contains("Length")) {
                String[] contentLengthString = postRequest[i].split(" ");
                contentLength = Integer.parseInt(contentLengthString[1].trim());
            }
            else if(postRequest[i].contains("Type")) {
                String[] contentTypeString = postRequest[i].split(" ");
                contentType = contentTypeString[1].trim();
            }
        }

        //find the content to add to file
        String message = "" + (char) character;
        int charc = 0;
        while(message.length() != contentLength) {
            charc = in.read();
            message = message + ((char) charc);
        }

        FileWriter writer = new FileWriter(getFile[1].substring(1));
        writer.write(message);
        writer.close();

        String response = "HTTP/1.1 200 OK" + CRLF + "Content-Type: " + contentType + CRLF + "Content-Length: " + Integer.toString(contentLength) + CRLF + CRLF + message;
        return response;
    }

    public static void handleRequest(Socket socket) {
        try {
            InputStream in = socket.getInputStream();
            //define output stream to write back to GET request
            OutputStream out = socket.getOutputStream();

            String requestType = "";
            final String CRLF = "\r\n";

            int ch = 0;
            while((ch = in.read()) != '\n') {
                requestType = requestType + ((char) ch);
            }
            System.out.println(requestType);

            String[] getFile = requestType.split(" ");
            File file = new File(getFile[1].substring(1));

            if(requestType.contains("GET")) {
                if(file.isFile()) {
                    String response = handleGetRequest(getFile, file, CRLF);
                    System.out.println(response);
                    out.write(response.getBytes());
                }
                else {
                    String error = "HTTP/1.1 404 Not Found" + CRLF + "File Not Found";
                    out.write(error.getBytes());  //return error code
                }
            }
            else if (requestType.contains("POST")) {
                if(file.isFile() && getFile[1].contains("txt")) {
                    String response = handlePostRequest(getFile, in, CRLF);
                    out.write(response.getBytes());
                }
                else {
                    String errorCode = "HTTP/1.1 405 Method Not Allowed" + CRLF + "Method Not Allowed";
                    out.write(errorCode.getBytes());
                }
            }
            else if (requestType.contains("PUT")) {
                //Create new file if file doesn't exist
                if(!file.isFile()) {
                    file.createNewFile();
                }
                // Overrides the content if the file already exists.
                String response = handlePutRequest(getFile, in, CRLF);
                out.write(response.getBytes());
            }
            else if (requestType.contains("DELETE")) {
                file.delete();    // delete the file
                String response = "HTTP/1.1 200 OK" + CRLF;
                out.write(response.getBytes());
            }
            else if (requestType.contains("HEAD")) {
                if(file.isFile()) {
                    String contentType = "";
                    if(getFile[1].contains("txt")) {
                        contentType = "text/plain";
                    }
                    else if(getFile[1].contains("jpg")) {
                        contentType = "image/jpeg";
                    }

                    int contentLength = (int) file.length();
                    String response = "HTTP/1.1 200 OK" + CRLF + "Content-Type: " + contentType + CRLF + "Content-Length: " + Integer.toString(contentLength) + CRLF + CRLF;
                    out.write(response.getBytes());
                    System.out.print(response);
                }
                else {
                    String error = "HTTP/1.1 404 Not Found" + CRLF + "File Not Found";
                    out.write(error.getBytes());
                }
            }

            in.close();
            out.close();
            socket.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

  public static void main(String[] args) throws Exception {
    ServerSocket server = new ServerSocket(80);
    System.out.println("Listening for connection on port 80 ...");
    Socket socket = null;

    while((socket = server.accept()) != null) {
        final Socket threadSocket = socket;
        new Thread( () -> handleRequest(threadSocket)).start();
    }

    System.out.println("closed");
    server.close();
  }
}