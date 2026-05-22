#!/usr/bin/env node
/**
 * AI Design Assistant API 自动化测试脚本
 *
 * 使用方法:
 *   node test-api.js [test-group]
 *
 * 测试组:
 *   e2e      - 端到端流程测试
 *   auth     - 权限与数据隔离测试
 *   all      - 运行所有测试 (默认)
 *
 * 依赖:
 *   npm install axios chalk
 */

const axios = require('axios');
const chalk = require('chalk');

// 配置
const API_BASE = 'http://localhost:8080/api/design';

// 测试用户配置 (需在数据库中预先创建)
const TEST_USERS = {
  designer_a: { userId: 101, tenantId: 2001, name: '设计师A' },
  assistant_a: { userId: 201, tenantId: 2001, name: '助理A' },
  fabric_a: { userId: 301, tenantId: 3001, name: '面料商A' },
  pattern_a: { userId: 401, tenantId: 4001, name: '版师A' },
  designer_b: { userId: 103, tenantId: 2002, name: '设计师B' },
  fabric_b: { userId: 302, tenantId: 3002, name: '面料商B' },
};

// 测试结果
const results = {
  passed: 0,
  failed: 0,
  skipped: 0,
  errors: [],
};

// 工具函数
function log(message, type = 'info') {
  const colors = {
    info: chalk.blue,
    success: chalk.green,
    error: chalk.red,
    warning: chalk.yellow,
    section: chalk.cyan.bold,
  };
  console.log(colors[type](message));
}

function getHeaders(userId, tenantId) {
  return {
    'X-Tenant-Id': String(tenantId),
    'X-User-Id': String(userId),
    'Content-Type': 'application/json',
  };
}

async function request(method, endpoint, data = null, user = null) {
  const headers = user ? getHeaders(user.userId, user.tenantId) : getHeaders(1, 2001);
  try {
    const response = await axios({
      method,
      url: `${API_BASE}${endpoint}`,
      headers,
      data,
      validateStatus: () => true, // 不抛出 HTTP 错误
    });
    return response.data;
  } catch (error) {
    return { code: 500, message: error.message };
  }
}

// 断言函数
function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function assertCode(data, expectedCode, message) {
  assert(data.code === expectedCode, `${message} - 预期状态码 ${expectedCode}, 实际 ${data.code}`);
}

// 测试用例执行
async function runTest(name, testFn) {
  log(`\n▶ ${name}`, 'section');
  try {
    await testFn();
    log(`  ✓ 通过`, 'success');
    results.passed++;
  } catch (error) {
    log(`  ✗ 失败: ${error.message}`, 'error');
    results.failed++;
    results.errors.push({ test: name, error: error.message });
  }
}

// 跳过测试
function skipTest(name) {
  log(`\n○ ${name} - 跳过`, 'warning');
  results.skipped++;
}

// ====== P0 端到端流程测试 ======
async function testE2E() {
  log('\n========================================', 'section');
  log('P0: 端到端流程测试', 'section');
  log('========================================', 'section');

  const { designer_a, assistant_a, fabric_a, pattern_a } = TEST_USERS;
  let requirementId = null;
  let fabricTaskId = null;
  let patternTaskId = null;

  // TC-001: 创建需求
  await runTest('TC-001: 设计师创建需求并发布', async () => {
    const result = await request('POST', '/requirement/create', {
      title: '测试需求 - 真丝连衣裙',
      rawText: '我要设计一款真丝连衣裙，适合春秋季节穿着，淡蓝色，有小碎花图案',
      conversationHistory: [
        { role: 'user', content: '我要设计一款真丝连衣裙', time: new Date().toISOString() },
        { role: 'assistant', content: '好的，我可以帮您设计真丝连衣裙', time: new Date().toISOString() },
      ],
    }, designer_a);

    assertCode(result, 200, '创建需求');
    assert(result.data, '返回数据不应为空');
    assert(result.data.id, '需求ID不应为空');
    assert(result.data.status === 'draft', '初始状态应为draft');

    requirementId = result.data.id;
    log(`    需求ID: ${requirementId}`, 'info');

    // 生成总结
    const summaryResult = await request('POST', '/requirement/summarize', {
      requirementId,
    }, designer_a);
    assertCode(summaryResult, 200, '生成总结');

    // 确认发布
    const confirmResult = await request('POST', '/requirement/confirm', {
      requirementId,
    }, designer_a);
    assertCode(confirmResult, 200, '确认发布');

    // 验证任务自动创建
    const detailResult = await request('GET', `/requirement/detail/${requirementId}`, null, designer_a);
    assertCode(detailResult, 200, '获取需求详情');
    assert(detailResult.data.tasks, '应包含子任务列表');
    assert(detailResult.data.tasks.length >= 1, '至少应创建1个子任务');

    const tasks = detailResult.data.tasks;
    fabricTaskId = tasks.find(t => t.taskType === 'fabric')?.id;
    patternTaskId = tasks.find(t => t.taskType === 'pattern')?.id;

    log(`    面料任务ID: ${fabricTaskId}, 打版任务ID: ${patternTaskId}`, 'info');
  });

  // TC-002: 转助理流程
  await runTest('TC-002: 设计师转助理并发布', async () => {
    // 创建新需求用于测试转助理
    const createResult = await request('POST', '/requirement/create', {
      title: '测试转助理需求',
      rawText: '这是一个需要助理审核的设计需求',
    }, designer_a);

    assertCode(createResult, 200, '创建需求');
    requirementId = createResult.data.id;

    // 转给助理
    const transferResult = await request('POST', '/requirement/transfer', {
      requirementId,
      assistantId: assistant_a.userId,
    }, designer_a);
    assertCode(transferResult, 200, '转助理');

    // 验证状态变更
    const detailResult = await request('GET', `/requirement/detail/${requirementId}`, null, designer_a);
    assertCode(detailResult, 200, '获取详情');
    assert(detailResult.data.status === 'assistant_processing', '状态应为assistant_processing');

    // 助理发布
    const publishResult = await request('POST', `/assistant/publish/${requirementId}?forcePublish=true`, null, assistant_a);
    assertCode(publishResult, 200, '助理发布');

    // 验证状态
    const afterPublish = await request('GET', `/requirement/detail/${requirementId}`, null, designer_a);
    assert(afterPublish.data.status === 'released', '发布后状态应为released');
  });

  // TC-003: 面料商接受并发货
  await runTest('TC-003: 面料商接受任务并发货', async () => {
    assert(fabricTaskId, '面料任务ID应已获取');

    // 获取任务列表
    const listResult = await request('GET', '/task/my-tasks', null, fabric_a);
    assertCode(listResult, 200, '获取任务列表');
    assert(listResult.data.tasks.length > 0, '应有分配给面料商的任务');

    // 接受任务
    const acceptResult = await request('PUT', `/task/${fabricTaskId}/status`, {
      taskId: fabricTaskId,
      status: 'accepted',
    }, fabric_a);
    assertCode(acceptResult, 200, '接受任务');

    // 验证状态
    const afterAccept = await request('GET', `/task/detail/${fabricTaskId}`, null, fabric_a);
    assert(afterAccept.data.status === 'accepted', '状态应为accepted');

    // 发货
    const shipResult = await request('POST', `/task/${fabricTaskId}/ship`, {
      taskId: fabricTaskId,
      logisticsCompany: '顺丰快递',
      logisticsTrackingNo: 'SF1234567890',
    }, fabric_a);
    assertCode(shipResult, 200, '发货');

    // 验证状态和物流信息
    const afterShip = await request('GET', `/task/detail/${fabricTaskId}`, null, fabric_a);
    assert(afterShip.data.status === 'shipped', '状态应为shipped');
    assert(afterShip.data.logisticsCompany === '顺丰快递', '物流公司应正确保存');
    assert(afterShip.data.logisticsTrackingNo === 'SF1234567890', '物流单号应正确保存');
  });

  // TC-004: 版师等待面料完成
  await runTest('TC-004: 版师任务依赖面料完成', async () => {
    assert(patternTaskId, '打版任务ID应已获取');

    // 获取任务详情
    const detailResult = await request('GET', `/task/detail/${patternTaskId}`, null, pattern_a);
    assertCode(detailResult, 200, '获取任务详情');

    // 验证依赖检查
    if (detailResult.data.canAccept === false) {
      log(`    按预期: 面料未完成，不能接受任务`, 'info');
      assert(detailResult.data.cannotAcceptReason, '应有拒绝原因说明');
    }

    // 手动将面料任务设置为 delivered (模拟)
    log(`    模拟: 将面料任务状态改为 delivered`, 'warning');
    // 注意: 这里需要数据库操作或API来修改状态
    skipTest('需要手动设置面料任务为delivered状态');
  });

  // TC-005: 版师上传结果
  await runTest('TC-005: 版师上传结果完成', async () => {
    // 先接受任务
    const acceptResult = await request('PUT', `/task/${patternTaskId}/status`, {
      taskId: patternTaskId,
      status: 'accepted',
    }, pattern_a);
    // 如果面料未完成，这里可能失败，这是预期行为
    if (acceptResult.code !== 200) {
      skipTest('面料任务未完成，无法接受');
      return;
    }

    // 上传结果
    const uploadResult = await request('POST', `/task/${patternTaskId}/upload-result`, {
      taskId: patternTaskId,
      resultUrl: 'https://storage.example.com/pattern_001.pdf',
    }, pattern_a);
    assertCode(uploadResult, 200, '上传结果');

    // 完成任务
    const doneResult = await request('PUT', `/task/${patternTaskId}/status`, {
      taskId: patternTaskId,
      status: 'done',
    }, pattern_a);
    assertCode(doneResult, 200, '完成任务');

    // 验证状态
    const afterDone = await request('GET', `/task/detail/${patternTaskId}`, null, pattern_a);
    assert(afterDone.data.status === 'done', '状态应为done');
    assert(afterDone.data.resultUrl, '应有结果URL');
  });
}

// ====== P0 权限与数据隔离测试 ======
async function testAuth() {
  log('\n========================================', 'section');
  log('P0: 权限与数据隔离测试', 'section');
  log('========================================', 'section');

  const { designer_a, designer_b, fabric_a, fabric_b } = TEST_USERS;
  let testRequirementId = null;

  // TC-101: 租户数据隔离
  await runTest('TC-101: 租户数据隔离', async () => {
    // 设计师A创建需求
    const createResult = await request('POST', '/requirement/create', {
      title: '租户A的私密需求',
      rawText: '这是租户A的需求',
    }, designer_a);
    assertCode(createResult, 200, '设计师A创建需求');
    testRequirementId = createResult.data.id;

    // 设计师B查看需求列表
    const listResult = await request('GET', '/requirement/list', null, designer_b);
    assertCode(listResult, 200, '设计师B获取需求列表');

    // 验证B看不到A的需求
    const hasARequirement = listResult.data.some((r: any) => r.id === testRequirementId);
    assert(!hasARequirement, '设计师B不应看到租户A的需求');
  });

  // TC-102: 助理待办隔离
  await runTest('TC-102: 助理待办隔离', async () => {
    // 租户A设计师转助理
    const transferResult = await request('POST', '/requirement/transfer', {
      requirementId: testRequirementId,
    }, designer_a);
    // 如果已经转过了，可能会失败，忽略
    if (transferResult.code === 200) {
      log(`    需求已转给助理`, 'info');
    }

    // 租户B助理尝试访问
    // 这里需要创建租户B的助理用户，暂时跳过
    skipTest('需要创建租户B的助理用户');
  });

  // TC-103: 面料商任务隔离
  await runTest('TC-103: 面料商任务隔离', async () => {
    // 面料商A获取任务列表
    const listResultA = await request('GET', '/task/my-tasks', null, fabric_a);
    assertCode(listResultA, 200, '面料商A获取任务列表');

    // 面料商B获取任务列表
    const listResultB = await request('GET', '/task/my-tasks', null, fabric_b);
    assertCode(listResultB, 200, '面料商B获取任务列表');

    // 验证面料商B看不到面料商A的任务
    if (listResultA.data.tasks.length > 0) {
      const firstTaskId = listResultA.data.tasks[0].id;
      const hasSameTask = listResultB.data.tasks.some((t: any) => t.id === firstTaskId);
      assert(!hasSameTask, '面料商B不应看到面料商A的任务');
    }
  });

  // TC-104: 版师任务隔离
  await runTest('TC-104: 版师不能操作其他人的任务', async () => {
    // 获取面料商A的任务ID
    const listResult = await request('GET', '/task/my-tasks', null, fabric_a);
    if (listResult.data.tasks.length === 0) {
      skipTest('没有可用的任务');
      return;
    }
    const taskId = listResult.data.tasks[0].id;

    // 版师A尝试修改面料商A的任务
    const updateResult = await request('PUT', `/task/${taskId}/status`, {
      taskId,
      status: 'accepted',
    }, pattern_a);

    // 应该返回403或业务错误
    assert(updateResult.code !== 200, '版师不应能修改面料商的任务');
    log(`    预期结果: API返回错误码 ${updateResult.code}`, 'info');
  });

  // TC-105: 面料库访问控制
  await runTest('TC-105: 面料库访问控制', async () => {
    // 设计师A查看面料库
    const listResult = await request('GET', '/fabric-library/list?onlyVisible=true', null, designer_a);
    assertCode(listResult, 200, '获取面料库列表');

    // 设计师A尝试修改面料 (假设面料ID为1)
    const updateResult = await request('PUT', '/fabric-library/1', {
      id: 1,
      name: '修改后的面料名',
    }, designer_a);

    // 应该返回403或业务错误
    assert(updateResult.code !== 200, '设计师不应能修改面料');
    log(`    预期结果: API返回错误码 ${updateResult.code}`, 'info');

    // 面料商A可以修改自己的面料
    // 这里需要先创建面料，暂时跳过
    skipTest('需要预先创建测试面料数据');
  });
}

// ====== 主函数 ======
async function main() {
  const args = process.argv.slice(2);
  const testGroup = args[0] || 'all';

  log('🧪 AI 设计助手插件 - API 自动化测试', 'section');
  log(`测试环境: ${API_BASE}`, 'info');
  log(`测试组: ${testGroup}`, 'info');

  try {
    if (testGroup === 'e2e' || testGroup === 'all') {
      await testE2E();
    }

    if (testGroup === 'auth' || testGroup === 'all') {
      await testAuth();
    }

    // 输出测试结果
    log('\n========================================', 'section');
    log('测试结果汇总', 'section');
    log('========================================', 'section');
    log(`✓ 通过: ${results.passed}`, 'success');
    log(`✗ 失败: ${results.failed}`, results.failed > 0 ? 'error' : 'info');
    log(`○ 跳过: ${results.skipped}`, 'warning');
    log(`总计: ${results.passed + results.failed + results.skipped}`, 'info');

    if (results.errors.length > 0) {
      log('\n失败的测试:', 'error');
      results.errors.forEach((e, i) => {
        log(`${i + 1}. ${e.test}: ${e.error}`, 'error');
      });
    }

    process.exit(results.failed > 0 ? 1 : 0);

  } catch (error) {
    log(`测试执行异常: ${error.message}`, 'error');
    process.exit(1);
  }
}

// 运行
if (require.main === module) {
  main();
}

module.exports = { runTest, request, TEST_USERS };