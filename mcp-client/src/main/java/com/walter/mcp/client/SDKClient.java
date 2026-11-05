package com.walter.mcp.client;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SDKClient {
    private final McpClientTransport transport;

    public SDKClient(McpClientTransport transport) {
        this.transport = transport;
    }

    public static void main(String[] args) {
        // Create SSE transport pointing to the MCP server
        var transport = new WebFluxSseClientTransport(
                WebClient.builder().baseUrl("http://localhost:8080")
        );

        // Create and run the client
        new SDKClient(transport).run();
    }

    public void run() {
        try {
            // Create synchronous MCP client
            var client = McpClient.sync(this.transport).build();
            // Initialize the connection
            client.initialize();
            System.out.println("✅ Connected to MCP server successfully!");

            // Verify connection with ping
            client.ping();
            System.out.println("✅ Server ping successful!");

            McpSchema.ListToolsResult toolsList = client.listTools();
            System.out.println("\n📋 Available Tools:");
            toolsList.tools().forEach(tool -> {
                System.out.println("  - " + tool.name() + ": " + tool.description());
            });

            // Test calculator operations
            System.out.println("\n🧮 Testing Calculator Operations:");

            // Addition
            CallToolResult resultAdd = client.callTool(
                    new McpSchema.CallToolRequest("add", Map.of("a", 5.0, "b", 3.0))
            );
            System.out.println("Add 5 + 3 = " + extractResult(resultAdd));

            // Help
            CallToolResult resultHelp = client.callTool(
                    new McpSchema.CallToolRequest("help", Map.of())
            );
            System.out.println("\n📖 Help Information:");
            System.out.println(extractResult(resultHelp));

        } catch (Exception e) {
            System.err.println("❌ Error running MCP client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extract the text result from a CallToolResult
     */
    private String extractResult(CallToolResult result) {
        if (result == null || result.content() == null || result.content().isEmpty()) {
            return "No result";
        }

        String text = result.content().stream()
                .filter(McpSchema.TextContent.class::isInstance)          // 只取文本块
                .map(c -> ((McpSchema.TextContent) c).text())
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("\n"));

        if (Boolean.TRUE.equals(result.isError())) {
            return "Error: " + (text.isBlank() ? "unknown" : text);
        }
        return text.isBlank() ? "No result" : text;
    }

}
