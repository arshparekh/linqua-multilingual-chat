# translation-chat
Live on:https://linqua-real-time-translation-chat.onrender.com

## Prerequisites
- Java 17+
- Node.js installed

## Node sidecar (free translation)
This app can translate using a Node script with the npm package:
`google-translate-api-x`

### 1) Install Node dependency
From project root (where `package.json` is):

```bash
npm install
```

### 2) Configure Node path
In `src/main/resources/application.properties`:
- `node.translation.node-executable`
- `node.translation.script`

Example (Windows):
- `node.translation.node-executable=C:\\Program Files\\nodejs\\node.exe`
- `node.translation.script=node/translate.js`

## Run
```bash
mvn -DskipTests spring-boot:run
```
Server starts on http://localhost:8081

