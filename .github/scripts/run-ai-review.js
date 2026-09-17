const fs = require('fs');
const path = require('path');
const https = require('https');
const { Octokit } = require('@octokit/action');

// 1. 설정 및 파라미터 파싱
const diffPath = process.argv[2];
if (!diffPath || !fs.existsSync(diffPath)) {
  console.error("Error: Diff file path is missing or does not exist.");
  process.exit(1);
}

const diffContent = fs.readFileSync(diffPath, 'utf8');
if (!diffContent.trim()) {
  console.log("No code changes detected. Skipping AI review.");
  process.exit(0);
}

const apiKey = process.env.OPENAI_API_KEY;
if (!apiKey) {
  console.error("Error: OPENAI_API_KEY environment variable is not defined.");
  process.exit(1);
}

// 2. 동적 규칙(Rules Partitioning) 로드 로직
let commonRules = '';
let backendRules = '';
let frontendRules = '';

// 기본 경로 설정 (로컬 실행 및 CI 실행 호환성 확보)
const baseDir = path.resolve(__dirname, '../../');
const commonRulesPath = path.join(baseDir, 'rules/common/project-rules.md');
const backendRulesPath = path.join(baseDir, 'rules/backend/backend-rules.md');
const frontendRulesPath = path.join(baseDir, 'rules/frontend/frontend-rules.md');

try {
  if (fs.existsSync(commonRulesPath)) {
    commonRules = fs.readFileSync(commonRulesPath, 'utf8');
  }
} catch (e) {
  console.warn("Warning: Could not read common rules.", e.message);
}

// Diff를 통해 변경된 영역 감지
const hasBackendChanges = diffContent.includes('aims-backend/');
const hasFrontendChanges = diffContent.includes('frontend/');

console.log(`Detected changes - Backend: ${hasBackendChanges}, Frontend: ${hasFrontendChanges}`);

if (hasBackendChanges) {
  try {
    if (fs.existsSync(backendRulesPath)) {
      backendRules = fs.readFileSync(backendRulesPath, 'utf8');
      console.log("Loaded Backend rules for dynamic injection.");
    }
  } catch (e) {
    console.warn("Warning: Could not read backend rules.", e.message);
  }
}

if (hasFrontendChanges) {
  try {
    if (fs.existsSync(frontendRulesPath)) {
      frontendRules = fs.readFileSync(frontendRulesPath, 'utf8');
      console.log("Loaded Frontend rules for dynamic injection.");
    }
  } catch (e) {
    console.warn("Warning: Could not read frontend rules.", e.message);
  }
}

// 3. AI 프롬프트 구성
let dynamicRulesPrompt = `=== 1. COMMON RULES ===\n${commonRules || 'No common rules available.'}\n\n`;

if (hasBackendChanges && backendRules) {
  dynamicRulesPrompt += `=== 2. BACKEND RULES ===\n${backendRules}\n\n`;
}
if (hasFrontendChanges && frontendRules) {
  dynamicRulesPrompt += `=== 3. FRONTEND RULES ===\n${frontendRules}\n\n`;
}

const systemPrompt = `You are a high-level AI Code Reviewer for the AIMS-Graph project.
Your primary task is to review the code changes (git diff) and strictly enforce the project's rules.

Here are the active project rules injected dynamically based on the files you are reviewing:
${dynamicRulesPrompt}

For each modified file, verify the diff against these active rules. Pay extra attention to:
1. NO RAG: Absolutely reject any RAG, vector search, or vector embedding library configurations.
2. TDD Guard: Verify corresponding test files are created/modified for new backend/frontend implementations.
3. Thread Pinning (Backend): Check if 'synchronized' is used in blocking Java code, suggest 'ReentrantLock'.
4. UI/UX Slop (Frontend): Check if generic layouts, bad styling, or uncurated colors violate UI_GUIDE.md.

Your output should be structured as follows:
## 🤖 AIMS-Graph AI Code Review Report (Smart Partitioned)
- **Overall Status**: [PASS / FAIL] (If any critical rule is violated, mark as FAIL. Otherwise, PASS)
- **Checklist Review**:
  1. **Common Rules & NO RAG**: [PASS / FAIL] - Brief reason
  ${hasBackendChanges ? `2. **Backend Architecture & VT**: [PASS / FAIL] - Brief reason\n` : ''}  ${hasFrontendChanges ? `3. **Frontend UX & Styling**: [PASS / FAIL] - Brief reason\n` : ''}  4. **TDD Guard**: [PASS / FAIL] - Brief reason
- **Detailed Feedback / Suggestions**: Provide actionable suggestions for any FAIL or code smell. Keep it constructive.

Respond in Korean (한국어로 작성해주세요).`;

const requestData = JSON.stringify({
  model: "gpt-4o-mini",
  messages: [
    { role: "system", content: systemPrompt },
    { role: "user", content: `Here is the git diff for review:\n\n\`\`\`diff\n${diffContent}\n\`\`\`` }
  ],
  temperature: 0.2
});

// 4. OpenAI API 호출
console.log("Requesting Smart AI review from OpenAI...");
const options = {
  hostname: 'api.openai.com',
  port: 443,
  path: '/v1/chat/completions',
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${apiKey}`,
    'Content-Length': Buffer.byteLength(requestData)
  }
};

const req = https.request(options, (res) => {
  let responseData = '';

  res.on('data', (chunk) => {
    responseData += chunk;
  });

  res.on('end', async () => {
    if (res.statusCode !== 200) {
      console.error(`Error: OpenAI API returned status code ${res.statusCode}`);
      console.error(responseData);
      process.exit(1);
    }

    try {
      const jsonResponse = JSON.parse(responseData);
      const aiFeedback = jsonResponse.choices[0].message.content;

      console.log("\n=== AI Code Review Feedback ===");
      console.log(aiFeedback);
      console.log("===============================\n");

      // PR 코멘트 등록 (CI 환경인 경우)
      await postPrComment(aiFeedback);
      
    } catch (parseError) {
      console.error("Error parsing OpenAI response json:", parseError);
      process.exit(1);
    }
  });
});

req.on('error', (e) => {
  console.error("OpenAI request failed:", e);
  process.exit(1);
});

req.write(requestData);
req.end();

async function postPrComment(feedbackBody) {
  if (!process.env.GITHUB_REPOSITORY || !process.env.GITHUB_REF) {
    console.log("Not in GitHub CI environment. Skipping PR comment posting.");
    return;
  }

  const prParts = process.env.GITHUB_REF.split('/');
  if (prParts[1] !== 'pull') {
    console.log("Not a Pull Request. Skipping comment posting.");
    return;
  }

  const prNumber = parseInt(prParts[2], 10);
  const [owner, repo] = process.env.GITHUB_REPOSITORY.split('/');

  console.log(`Posting AI Review feedback to PR #${prNumber} on ${owner}/${repo}...`);

  try {
    const octokit = new Octokit();
    await octokit.issues.createComment({
      owner,
      repo,
      issue_number: prNumber,
      body: feedbackBody
    });
    console.log("Successfully posted PR comment.");
  } catch (err) {
    console.error("Failed to post comment on PR via GitHub API:", err);
  }
}
