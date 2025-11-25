# wardenai
This is a mincraft java plugin to enagle private chat within the game with a dedicated LLM agent. 
The repo create a jar file for the minecraft java edition. The plugin support vesion paper 1.21.10.
The plugin add a chat interface inside the game where you can chat with 'wanrdenai', a AI agent that help you nevigate the game, advise you and know about all there is to know about minecraft. The story, the caracters and commands. 
On the server, the plugin take the player query add a knowladge-base file, pre-made, and attach it to the prompt and send it to the llm. 

Setting for the server to connect to Groq LLM, use the following method:
``` javascript 
import { Groq } from 'groq-sdk';

const groq = new Groq();

const chatCompletion = await groq.chat.completions.create({
  "messages": [
    {
      "role": "user",
      "content": ""
    }
  ],
  "model": "openai/gpt-oss-20b",
  "temperature": 1,
  "max_completion_tokens": 8192,
  "top_p": 1,
  "stream": true,
  "reasoning_effort": "medium",
  "stop": null
});

for await (const chunk of chatCompletion) {
  process.stdout.write(chunk.choices[0]?.delta?.content || '');
}
```
if the user run out token creadit. wanrdenai tall him his is sorry but he can no longer help him in the game. 
