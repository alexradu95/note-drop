import { GoogleGenAI } from "@google/genai";

// We use Gemini to demonstrate what the Android app does: 
// Takes raw, messy input and formats it perfectly for Obsidian/Evernote.

export const processRawCapture = async (rawText: string): Promise<string> => {
  try {
    const apiKey = process.env.API_KEY;
    if (!apiKey) {
      return "API Key not found. Please ensure the environment is configured correctly.";
    }

    const ai = new GoogleGenAI({ apiKey });
    
    const response = await ai.models.generateContent({
      model: 'gemini-2.5-flash',
      contents: `
        You are the backend engine of a high-speed note-taking app. 
        Your goal is to take a raw, potentially messy "brain dump" string and format it 
        into a clean Markdown note suitable for apps like Obsidian.
        
        Rules:
        1. Extract a concise Title.
        2. Add relevant #tags.
        3. Fix grammar and formatting.
        4. Keep the tone of the original thought.
        
        Raw Input: "${rawText}"
        
        Output Format (Markdown):
        # [Title]
        
        [Cleaned Content]
        
        ---
        **Tags:** [List of tags]
        **Captured:** [Current Date]
      `,
    });

    return response.text || "Could not generate response.";
  } catch (error) {
    console.error("Gemini processing error:", error);
    return `Error processing note: ${(error as Error).message}. \n\n(Fallback): \n# Note\n${rawText}`;
  }
};