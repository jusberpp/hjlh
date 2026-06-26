<template>
  <main class="tifenbao-page">
    <section class="hero">
      <div class="hero__copy">
        <span class="eyebrow">E学慧通 · 暑期课程 H5 闭环</span>
        <h1>从登录验证到提分宝下载</h1>
        <p>
          首屏为手机号和姓名登录验证；未认证学生可进入注册认证流程，认证后回到学生主页查看课程、讲义、作业任务和提分宝进度。
        </p>
      </div>
      <div class="hero__map">
        <span v-for="item in fullFlowNodes" :key="item">{{ item }}</span>
      </div>
    </section>

    <section class="mode-switch" aria-label="演示端切换">
      <button
        type="button"
        :class="{ active: activeMode === 'student' }"
        @click="activeMode = 'student'"
      >
        学生端完整流程
      </button>
      <button
        type="button"
        :class="{ active: activeMode === 'admin' }"
        @click="activeMode = 'admin'"
      >
        运营后台
      </button>
    </section>

    <section v-if="activeMode === 'student'" class="student-workbench">
      <aside class="phone-shell" aria-label="学生端操作页">
        <header class="mini-header">
          <span>9:41</span>
          <strong>{{ currentStudentStep?.label }}</strong>
          <span>•••</span>
        </header>

        <div class="phone-body">
          <section v-if="studentStep === 'login'" class="screen-block login-screen">
            <span class="screen-kicker">登录验证</span>
            <h2>输入手机号和姓名即可登录</h2>
            <p class="muted">已完成认证的学生，可用认证时绑定的手机号和姓名直接进入学生主页。</p>
            <label class="field">
              <span>手机号</span>
              <input v-model="loginForm.phone" type="tel" maxlength="11" inputmode="numeric" />
            </label>
            <label class="field">
              <span>学生姓名</span>
              <input v-model="loginForm.name" type="text" maxlength="20" />
            </label>
            <button class="primary-btn" type="button" @click="loginStudent">
              登录
            </button>
            <button class="register-cta" type="button" @click="goStep('verify')">
              还未认证？注册认证
            </button>
          </section>

          <section v-else-if="studentStep === 'verify'" class="screen-block">
            <span class="screen-kicker">学员身份认证</span>
            <h2>先确认你是本次暑期课程学员</h2>
            <p class="muted">注册认证沿用原 H5 的学校、年级、姓名、学号验证结构；已认证学生再次提交会提示下次可直接登录。</p>
            <label class="field">
              <span>学校名称</span>
              <input v-model="authForm.school" type="text" />
            </label>
            <label class="field">
              <span>年级</span>
              <input v-model="authForm.grade" type="text" />
            </label>
            <label class="field">
              <span>学生姓名</span>
              <input v-model="authForm.name" type="text" />
            </label>
            <label class="field">
              <span>学号</span>
              <input v-model="authForm.studentId" type="text" />
            </label>
            <button class="primary-btn" type="button" @click="verifyStudent">
              验证信息
            </button>
          </section>

          <section v-else-if="studentStep === 'confirm'" class="screen-block">
            <span class="screen-kicker">确认报名信息</span>
            <h2>请核对课程和学员信息</h2>
            <div class="info-panel">
              <div><span>学校：</span><strong>{{ authForm.school }}</strong></div>
              <div><span>年级：</span><strong>{{ authForm.grade }}</strong></div>
              <div><span>姓名：</span><strong>{{ authForm.name }}</strong></div>
              <div><span>学号：</span><strong>{{ authForm.studentId }}</strong></div>
            </div>
            <div class="course-open-card">
              <span>已报名课程</span>
              <strong>2026 高考假期黄金窗口学科能力跃迁课</strong>
              <small>含课程、配套资料、智能作业批改和提分宝下载服务</small>
            </div>
            <button class="primary-btn" type="button" @click="goStep('bindPhone')">
              立即开通看课权限
            </button>
            <button class="link-btn" type="button" @click="goStep('verify')">
              返回修改信息
            </button>
          </section>

          <section v-else-if="studentStep === 'bindPhone'" class="screen-block">
            <span class="screen-kicker">绑定看课手机号</span>
            <h2>绑定后用于看课与文件下载验证</h2>
            <div class="warning-card">
              <strong>严重提示</strong>
              <span>该手机号将作为小鹅通登录凭证，也用于本系统提分宝下载身份验证。</span>
            </div>
            <template v-if="phoneBindMode === 'wechat'">
              <button class="wechat-phone-btn" type="button" @click="getWechatPhone">
                微信一键获取手机号
              </button>
              <div v-if="wechatPhoneCaptured" class="account-card phone-result-card">
                <span>已获取微信手机号</span>
                <strong>{{ maskedPhone }}</strong>
                <small>请确认使用该手机号作为看课与文件下载验证账号。</small>
              </div>
              <button
                class="primary-btn"
                :class="{ disabled: !wechatPhoneCaptured }"
                type="button"
                :disabled="!wechatPhoneCaptured"
                @click="bindPhone"
              >
                确认提交并开通
              </button>
              <button class="link-btn" type="button" @click="switchPhoneBindMode('manual')">
                手动输入手机号
              </button>
            </template>
            <template v-else>
              <label class="field">
                <span>看课手机号</span>
                <input v-model="authForm.phone" type="tel" maxlength="11" />
              </label>
              <label class="field">
                <span>短信验证码</span>
                <div class="verification-code-row">
                  <input
                    v-model="phoneVerificationCode"
                    type="tel"
                    maxlength="6"
                    inputmode="numeric"
                    placeholder="请输入 6 位验证码"
                  />
                  <button
                    type="button"
                    :disabled="smsCountdown > 0"
                    @click="sendPhoneVerificationCode"
                  >
                    {{ smsCountdown > 0 ? `${smsCountdown}s 后重发` : "获取验证码" }}
                  </button>
                </div>
              </label>
              <div v-if="verificationCodeSent" class="verification-tip">
                验证码已发送至 {{ maskedPhone }}，演示验证码为 123456。
              </div>
              <button
                class="primary-btn"
                :class="{ disabled: !canVerifyPhone }"
                type="button"
                :disabled="!canVerifyPhone"
                @click="bindPhone"
              >
                验证并同步至学员信息
              </button>
              <button class="link-btn" type="button" @click="switchPhoneBindMode('wechat')">
                使用微信获取手机号
              </button>
            </template>
            <div class="qr-strip">
              <img :src="authQrImage" alt="客服二维码" />
              <span>如遇无法登录、课程未到账等问题，请联系专属辅导老师。</span>
            </div>
          </section>

          <section v-else-if="studentStep === 'success'" class="screen-block success-screen">
            <img class="success-img" :src="successImage" alt="课程已成功开通" />
            <h2>课程已成功开通！</h2>
            <p class="muted">请使用下方手机号登录小鹅通学习，并在学生主页查看课程、讲义、作业和提分宝进度。</p>
            <div class="account-card">
              <span>听课账号</span>
              <strong>{{ authForm.phone }}</strong>
              <small v-if="phoneSyncedToStudent">手机号已通过验证码校验，并同步至学员信息。</small>
            </div>
            <button class="primary-btn" type="button" @click="goStep('home')">
              进入学生主页
            </button>
          </section>

          <section v-else-if="studentStep === 'home'" class="screen-block home-screen">
            <div class="identity-card">
              <div class="avatar">慧</div>
              <div>
                <strong>{{ authForm.name }}</strong>
                <span>{{ authForm.grade }}暑期班 · {{ maskedPhone }}</span>
              </div>
            </div>

            <section class="home-course-hero">
              <div class="section-head">
                <strong>已开通课程</strong>
              </div>
              <div class="course-main-entry" data-testid="home-course-entry">
                <span>2026 高考假期黄金窗口学科能力跃迁课</span>
                <strong>课程学习权限已开通</strong>
                <small>高三年级 · 数学 / 物理 / 英语 / 语文</small>
              </div>
              <div class="subject-resource-list">
                <article
                  v-for="resource in subjectResources"
                  :key="resource.subject"
                  class="subject-resource-card"
                  :class="`subject-resource-card--${resource.tone}`"
                >
                  <div class="subject-resource-head">
                    <span class="subject-badge compact">{{ resource.subject }}</span>
                    <div>
                      <strong>{{ resource.topic }}</strong>
                      <small>{{ resource.teacher }}</small>
                    </div>
                    <button class="course-study-btn" type="button" @click="openCourseLesson(resource.subject)">
                      去上课
                    </button>
                  </div>
                  <div class="lesson-reminder" :class="{ empty: !resource.nextLessonTime }">
                    <span>下次上课</span>
                    <strong>{{ resource.nextLessonTime }}</strong>
                  </div>
                  <div class="subject-resource-actions">
                    <button type="button" @click="goSubjectResource(resource.subject, 'materials')">
                      <em v-if="resource.hasNewMaterial" class="entry-badge">新</em>
                      <span>讲义</span>
                      <strong>{{ resource.materialStatus }}</strong>
                    </button>
                    <button type="button" @click="goSubjectResource(resource.subject, 'tasks')">
                      <em v-if="resource.hasNewHomework" class="entry-badge">新</em>
                      <span>作业</span>
                      <strong>{{ resource.homeworkStatus }}</strong>
                    </button>
                    <button type="button" @click="goSubjectResource(resource.subject, 'tifenbao')">
                      <em v-if="resource.hasNewTifenbao" class="entry-badge">新</em>
                      <span>提分宝</span>
                      <strong>{{ resource.tifenbaoStatus }}</strong>
                    </button>
                  </div>
                </article>
              </div>
            </section>

            <div class="todo-panel">
              <div v-if="subjectTodoItems.length" class="todo-summary">
                <strong>按学科提醒</strong>
                <span>所有待处理内容都归属到具体学科，进入后直接操作对应讲义、作业或提分宝。</span>
              </div>
              <div
                v-for="item in subjectTodoItems"
                :key="`${item.subject}-${item.title}`"
                class="todo-item"
                :class="item.tone"
              >
                <strong>{{ item.subject }} · {{ item.title }}</strong>
                <span>{{ item.desc }}</span>
              </div>
            </div>
          </section>

          <section v-else-if="studentStep === 'materials'" class="screen-block">
            <span class="screen-kicker">{{ selectedSubjectResource.subject }} · 讲义资料</span>
            <h2>{{ selectedSubjectResource.topic }}</h2>
            <p class="muted">当前资料只展示本学科讲义，下载状态会回写到学生主页的对应学科卡片。</p>
            <div
              v-for="file in selectedMaterialFiles"
              :key="file.title"
              class="file-card"
            >
              <div>
                <strong>{{ file.title }}</strong>
                <span>{{ file.meta }}</span>
              </div>
              <button type="button" @click="downloadMaterial">
                {{ selectedSubjectResource.materialStatus === "已下载" ? "已下载" : "下载" }}
              </button>
            </div>
            <div v-if="!selectedMaterialFiles.length" class="empty-resource-card">
              <strong>暂无可查看资料</strong>
              <span>资料将在设置的显示时间到达后自动出现在列表中。</span>
            </div>
            <button class="secondary-btn" type="button" @click="goStep('home')">返回学生主页</button>
          </section>

          <section v-else-if="studentStep === 'tasks'" class="screen-block">
            <div class="course-context-card">
              <span>{{ selectedSubjectResource.subject }} · 作业任务</span>
              <strong>2026 高考假期黄金窗口学科能力跃迁课</strong>
              <small>{{ selectedSubjectResource.homeworkDesc }}</small>
            </div>
            <article
              v-for="task in selectedTaskCards"
              :key="task.title"
              class="task-card"
              :class="`task-card--${task.tone}`"
            >
              <div class="subject-mark">{{ task.subject }}</div>
              <div class="task-card__main">
                <strong>{{ task.title }}</strong>
                <span>{{ task.desc }}</span>
                <small>当前状态：{{ task.status }}</small>
              </div>
              <button type="button" @click="handleTaskAction(task.action)">
                {{ task.button }}
              </button>
            </article>
            <button class="secondary-btn" type="button" @click="goStep('home')">返回学生主页</button>
          </section>

          <section v-else-if="studentStep === 'submit'" class="screen-block">
            <span class="screen-kicker">物理学科 · 作业提交</span>
            <h2>物理暑期第 3 次作业</h2>
            <p class="muted">请上传完整物理作业图片或 PDF。截止前提交成功后，将进入提分宝生成中页面。</p>
            <div class="deadline-card" :class="{ overdue: isHomeworkOverdue }">
              <div>
                <strong>提交截止时间</strong>
                <span>{{ physicsHomeworkDeadline }}</span>
              </div>
              <em>{{ isHomeworkOverdue ? "已超时" : "未超时" }}</em>
            </div>
            <div class="upload-grid">
              <button v-for="file in uploadedFiles" :key="file" class="upload-tile uploaded" type="button">
                {{ file }}
              </button>
              <button class="upload-tile" type="button" @click="addMockFile">+ 添加</button>
            </div>
            <div class="notice-card" :class="{ danger: isHomeworkOverdue }">
              <strong>{{ isHomeworkOverdue ? "提交已关闭" : "提交后生成提分宝" }}</strong>
              <span>
                {{ isHomeworkOverdue ? "当前时间已晚于作业提交截止时间，提交按钮已禁用。" : "提交成功后系统立即进入提分宝生成流程，请在进度页等待发布。" }}
              </span>
            </div>
            <button
              class="primary-btn"
              :class="{ disabled: isHomeworkOverdue }"
              type="button"
              :disabled="isHomeworkOverdue"
              @click="submitHomework"
            >
              {{ isHomeworkOverdue ? "已超过提交截止时间" : "确认提交作业" }}
            </button>
            <button class="secondary-btn" type="button" @click="goStep('home')">返回学生主页</button>
          </section>

          <section v-else-if="studentStep === 'answer'" class="screen-block">
            <div class="result-icon success">✓</div>
            <h2>作业已提交，答案已开放</h2>
            <p class="muted">
              已收到 {{ uploadedFiles.length }} 个物理作业文件，请对照答案自行批改。
            </p>
            <div class="file-card">
              <div>
                <strong>{{ selectedSubjectResource.answerTitle }}</strong>
                <span>标准答案 · PDF</span>
              </div>
              <button type="button">查看</button>
            </div>
            <div v-if="homeworkSubmittedLate" class="notice-card danger">
              <strong>已超时提交</strong>
              <span>本次物理作业无法生成提分宝，系统已禁止录入小题分。</span>
            </div>
            <button
              class="primary-btn warning"
              :class="{ disabled: homeworkSubmittedLate }"
              type="button"
              :disabled="homeworkSubmittedLate"
              @click="goStep('score')"
            >
              {{ homeworkSubmittedLate ? "超时提交，无法录入小题分" : "填写小题分" }}
            </button>
            <button class="secondary-btn" type="button" @click="goStep('home')">返回学生主页</button>
          </section>

          <section v-else-if="studentStep === 'score'" class="screen-block score-screen">
            <div v-if="homeworkSubmittedLate" class="notice-card danger">
              <strong>小题分录入已关闭</strong>
              <span>物理作业超出截止时间提交，本次作业不进入提分宝生成流程。</span>
            </div>
            <div class="score-summary">
              <span>已填总分</span>
              <strong>{{ scoreTotal }} / {{ fullTotal }}</strong>
            </div>
            <div class="score-list">
              <label v-for="item in scoreItems" :key="item.no" class="score-row">
                <span>第 {{ item.no }} 题</span>
                <input v-model.number="item.score" type="number" min="0" :max="item.full" />
                <em>/ {{ item.full }} 分</em>
              </label>
            </div>
            <p v-if="scoreError" class="error-text">{{ scoreError }}</p>
            <div class="action-row">
              <button type="button" class="secondary-btn" :disabled="homeworkSubmittedLate" @click="saveDraft">保存草稿</button>
              <button type="button" class="primary-btn" :class="{ disabled: homeworkSubmittedLate }" :disabled="homeworkSubmittedLate" @click="submitScore">提交小题分</button>
            </div>
          </section>

          <section v-else-if="studentStep === 'waiting'" class="screen-block">
            <div class="result-icon waiting">⌛</div>
            <h2>{{ selectedSubjectResource.subject }}提分宝{{ selectedSubjectResource.tifenbaoStatus }}</h2>
            <p class="muted">{{ selectedSubjectResource.tifenbaoHint }}</p>
            <button class="primary-btn disabled" type="button" disabled>{{ selectedSubjectResource.tifenbaoStatus }}</button>
            <ol class="timeline">
              <li class="done">{{ selectedSubjectResource.homeworkStatus }} <span>{{ selectedSubjectResource.timelineStart }}</span></li>
              <li>系统整理作业数据 <span>{{ selectedSubjectResource.exportStatus }}</span></li>
              <li>学科网生成提分宝 <span>等待中</span></li>
              <li>发布下载 <span>等待中</span></li>
            </ol>
            <button class="secondary-btn" type="button" @click="goStep('home')">返回学生主页</button>
          </section>

          <section v-else class="screen-block">
            <div class="result-icon success">✓</div>
            <h2>提分宝已生成</h2>
            <p class="muted">
              {{ selectedSubjectResource.reportTitle }}_{{ authForm.name }}_提分宝.pdf<br />
              生成时间：07-20 16:38 · 有效期至 08-20
            </p>
            <button class="primary-btn success-btn" type="button" @click="downloadReport">
              下载提分宝
            </button>
            <div class="notice-card">
              <strong>下载记录</strong>
              <span>{{ hasDownloaded ? "最近下载：07-20 17:12 · 微信内打开" : "暂无下载记录" }}</span>
            </div>
            <button class="secondary-btn" type="button" @click="goStep('home')">返回学生主页</button>
          </section>
        </div>
      </aside>

      <aside class="student-side-panel">
        <h2>完整学生端路径</h2>
        <p>这不是孤立的提分宝页，而是把原项目全部学生端页面串成可评审闭环。</p>
        <div class="step-list">
          <button
            v-for="step in studentSteps"
            :key="step.key"
            type="button"
            :class="{ active: step.key === studentStep }"
            @click="goStep(step.key)"
          >
            <span>{{ step.index }}</span>
            {{ step.label }}
          </button>
        </div>
        <button class="publish-demo" type="button" @click="publishTifenbao">
          模拟后台发布提分宝
        </button>
      </aside>
    </section>

    <section v-else class="admin-console admin-pc">
      <aside class="admin-sidebar">
        <div class="admin-brand">E学慧通<br /><span>提分宝后台</span></div>
        <button
          v-for="tab in adminTabs"
          :key="tab.key"
          type="button"
          :class="{ active: activeAdminTab === tab.key }"
          @click="activeAdminTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </aside>

      <section class="admin-main">
        <header class="admin-header">
          <div>
            <h2>{{ activeAdminLabel }}</h2>
            <p>围绕学生主页的课程学习、讲义资料、作业任务和提分宝下载，完成配置、发布、导出和追踪。</p>
          </div>
          <button type="button" @click="showToast('后台配置已保存为草稿')">保存配置</button>
        </header>

        <div v-if="activeAdminTab === 'overview'" class="admin-stack">
          <div class="metrics">
            <article v-for="metric in overviewMetrics" :key="metric.label">
              <span>{{ metric.label }}</span>
              <strong :class="metric.tone">{{ metric.value }}</strong>
              <small>{{ metric.desc }}</small>
            </article>
          </div>
          <section class="admin-card">
            <div class="table-toolbar">
              <h3>当前学生端闭环</h3>
              <button type="button">查看学生端预览</button>
            </div>
            <div class="ops-flow">
              <article v-for="node in opsFlowNodes" :key="node.title" :class="node.tone">
                <span>{{ node.step }}</span>
                <strong>{{ node.title }}</strong>
                <small>{{ node.desc }}</small>
              </article>
            </div>
          </section>
          <section class="admin-card">
            <div class="table-toolbar">
              <h3>待处理事项</h3>
              <button type="button">一键提醒</button>
            </div>
            <AdminTable :columns="todoColumns" :rows="todoRows" />
          </section>
        </div>

        <div v-else-if="activeAdminTab === 'students'" class="admin-stack">
          <section class="admin-card">
            <div class="table-toolbar">
              <h3>学员认证与权限</h3>
              <div>
                <button type="button" @click="showToast('打开单个新增学员表单')">单个新增</button>
                <button type="button" @click="showToast('下载导入模板并批量导入学员')">批量导入表格</button>
              </div>
            </div>
            <div class="student-filter-panel">
              <label>
                <span>学校</span>
                <select v-model="studentFilters.school">
                  <option value="">全部学校</option>
                  <option v-for="school in studentFilterOptions.schools" :key="school" :value="school">{{ school }}</option>
                </select>
              </label>
              <label>
                <span>年级</span>
                <select v-model="studentFilters.grade">
                  <option value="">全部年级</option>
                  <option v-for="grade in studentFilterOptions.grades" :key="grade" :value="grade">{{ grade }}</option>
                </select>
              </label>
              <label>
                <span>班级</span>
                <select v-model="studentFilters.className">
                  <option value="">全部班级</option>
                  <option v-for="className in studentFilterOptions.classes" :key="className" :value="className">{{ className }}</option>
                </select>
              </label>
              <label class="student-search-field">
                <span>姓名搜索</span>
                <input v-model.trim="studentFilters.name" type="search" placeholder="输入学生姓名" />
              </label>
              <button type="button" @click="resetStudentFilters">重置</button>
            </div>
            <div class="student-list-summary">
              <span>当前筛选 {{ filteredStudentRows.length }} 名学员</span>
              <span>手机号为空的学员可先保留权限，登录前需补充或完成注册认证。</span>
            </div>
            <AdminTable :columns="studentColumns" :rows="filteredStudentRows" />
          </section>
          <section class="admin-card">
            <div class="ops-checklist">
              <span>学员权限是全局前置条件</span>
              <span>授权课程决定学生主页展示哪些学科入口</span>
              <span>支持单个新增和批量导入表格维护名单</span>
            </div>
          </section>
        </div>

        <div v-else class="admin-stack">
          <div class="metrics">
            <article v-for="metric in courseMetrics" :key="metric.label">
              <span>{{ metric.label }}</span>
              <strong :class="metric.tone">{{ metric.value }}</strong>
              <small>{{ metric.desc }}</small>
            </article>
          </div>

          <template v-if="isCreatingCourse">
            <section class="admin-card course-detail-header">
              <div>
                <button class="link-btn" type="button" @click="cancelCreateCourse">返回课程列表</button>
                <span class="screen-kicker">新建课程</span>
                <h3>通过小鹅通课程链接自动生成配置</h3>
                <p>运营只需要粘贴课程链接，系统自动获取学生主页课程卡所需信息。</p>
              </div>
              <button type="button" @click="fetchXetCourseInfo">获取课程信息</button>
            </section>

            <section class="admin-card">
              <div class="table-toolbar">
                <h3>小鹅通课程链接</h3>
                <button type="button" @click="fetchXetCourseInfo">重新获取</button>
              </div>
              <div class="course-import-panel">
                <label>
                  <span>课程链接</span>
                  <input v-model="courseImportForm.url" type="url" />
                </label>
                <div class="ops-checklist">
                  <span>自动识别学科</span>
                  <span>自动同步课程专题</span>
                  <span>自动读取授课老师</span>
                  <span>自动生成最近上课提醒</span>
                  <span>自动绑定去上课入口</span>
                </div>
              </div>
            </section>

            <section v-if="courseImportFetched" class="admin-card">
              <div class="table-toolbar">
                <h3>已获取的学生端课程卡配置</h3>
                <button type="button">生成课程并保存</button>
              </div>
              <div class="course-config-grid">
                <div class="form-grid">
                  <label>学科名称<input :value="importedCourseCard.subject" readonly /></label>
                  <label>课程专题<input :value="importedCourseCard.topic" readonly /></label>
                  <label>授课老师<input :value="importedCourseCard.teacher" readonly /></label>
                  <label>最近上课提醒<input :value="importedCourseCard.nextLessonTime" readonly /></label>
                  <label>上课入口<input :value="importedCourseCard.entryStatus" readonly /></label>
                  <label>来源链接<input :value="courseImportForm.url" readonly /></label>
                </div>
                <div class="student-card-preview">
                  <span class="status-badge">学生主页预览</span>
                  <div class="preview-course-card">
                    <div class="preview-course-head">
                      <span>{{ importedCourseCard.subject }}</span>
                      <div>
                        <strong>{{ importedCourseCard.topic }}</strong>
                        <small>{{ importedCourseCard.teacher }}</small>
                      </div>
                      <button type="button">去上课</button>
                    </div>
                    <div class="lesson-reminder">
                      <span>最近上课</span>
                      <strong>{{ importedCourseCard.nextLessonTime }}</strong>
                    </div>
                    <div class="preview-actions">
                      <button
                        v-for="entry in importedCourseCardEntries"
                        :key="entry.name"
                        type="button"
                      >
                        <em v-if="entry.hasNew">新</em>
                        <span>{{ entry.name }}</span>
                        <strong>{{ entry.status }}</strong>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
              <AdminTable :columns="courseCardEntryColumns" :rows="importedCourseCardEntryRows" />
              <div class="publish-row">
                <p>保存后，该课程会出现在有权限学生的主页课程卡中。</p>
                <button type="button" @click="showToast('课程已生成，学生端卡片配置已保存')">确认生成课程</button>
              </div>
            </section>
          </template>

          <template v-else-if="!selectedAdminCourse">
            <section class="admin-card">
              <div class="table-toolbar">
                <h3>选择课程进入管理</h3>
                <div>
                  <button type="button">同步小鹅通课程</button>
                  <button type="button" @click="startCreateCourse">新建课程</button>
                </div>
              </div>
              <div class="course-entry-grid">
                <button
                  v-for="course in courseRows"
                  :key="course.学科"
                  type="button"
                  class="course-entry-card"
                  @click="openAdminCourse(course.学科)"
                >
                  <span>{{ course.学科 }}</span>
                  <strong>{{ course.学生端展示 }}</strong>
                  <small>资料：{{ course.讲义资料 }} · 作业：{{ course.作业入口 }} · 提分宝：{{ course.提分宝状态 }}</small>
                </button>
              </div>
            </section>

            <section class="admin-card">
              <div class="table-toolbar">
                <h3>课程管理总表</h3>
                <button type="button">批量检查学生端展示</button>
              </div>
              <AdminTable :columns="courseColumns" :rows="courseRows" />
            </section>
          </template>

          <template v-else>
            <section class="admin-card course-detail-header">
              <div>
                <button class="link-btn" type="button" @click="backToCourseList">返回课程列表</button>
                <span class="screen-kicker">课程详情</span>
                <h3>{{ selectedAdminCourse }}课程运营配置</h3>
                <p>{{ selectedAdminCourseRow?.学生端展示 }} · {{ selectedAdminCourseRow?.作业入口 }} · {{ selectedAdminCourseRow?.提分宝状态 }}</p>
              </div>
              <button type="button">发布当前课程配置</button>
            </section>

            <nav class="course-subnav" aria-label="课程功能">
              <button
                v-for="tab in courseSubTabs"
                :key="tab.key"
                type="button"
                :class="{ active: activeCourseSubTab === tab.key }"
                @click="activeCourseSubTab = tab.key"
              >
                {{ tab.label }}
              </button>
            </nav>

            <section v-if="activeCourseSubTab === 'profile'" class="admin-card">
              <div class="table-toolbar">
                <h3>{{ selectedAdminCourse }}学生端课程卡同步结果</h3>
                <button type="button">重新同步小鹅通</button>
              </div>
              <div class="ops-checklist">
                <span>课程基础信息来自小鹅通课程链接</span>
                <span>最近上课提醒由课表时间自动生成</span>
                <span>学生端去上课按钮绑定同一课程入口</span>
              </div>
              <div class="course-config-grid">
                <div class="form-grid">
                  <label>学科名称<input :value="selectedCourseHomeCard.subject" readonly /></label>
                  <label>课程专题<input :value="selectedCourseHomeCard.topic" readonly /></label>
                  <label>授课老师<input :value="selectedCourseHomeCard.teacher" readonly /></label>
                  <label>最近上课提醒<input :value="selectedCourseHomeCard.nextLessonTime" readonly /></label>
                  <label>上课入口<input value="小鹅通课程链接已绑定" readonly /></label>
                  <label>学生端展示状态<input :value="selectedAdminCourseRow?.学生端展示" readonly /></label>
                </div>
                <div class="student-card-preview">
                  <span class="status-badge">学生主页预览</span>
                  <div class="preview-course-card">
                    <div class="preview-course-head">
                      <span>{{ selectedCourseHomeCard.subject }}</span>
                      <div>
                        <strong>{{ selectedCourseHomeCard.topic }}</strong>
                        <small>{{ selectedCourseHomeCard.teacher }}</small>
                      </div>
                      <button type="button">去上课</button>
                    </div>
                    <div class="lesson-reminder">
                      <span>最近上课</span>
                      <strong>{{ selectedCourseHomeCard.nextLessonTime }}</strong>
                    </div>
                    <div class="preview-actions">
                      <button
                        v-for="entry in selectedCourseCardEntries"
                        :key="entry.name"
                        type="button"
                      >
                        <em v-if="entry.hasNew">新</em>
                        <span>{{ entry.name }}</span>
                        <strong>{{ entry.status }}</strong>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
              <AdminTable :columns="courseCardEntryColumns" :rows="selectedCourseCardEntryRows" />
            </section>

            <section v-else-if="activeCourseSubTab === 'materials'" class="admin-card">
              <div class="table-toolbar">
                <h3>{{ selectedAdminCourse }}课程资料</h3>
                <div>
                  <button type="button">上传讲义</button>
                  <button type="button">发布到学生端</button>
                </div>
              </div>
              <AdminTable :columns="materialColumns" :rows="selectedMaterialRows" />
            </section>

            <section v-else-if="activeCourseSubTab === 'tasks'" class="admin-card form-card">
              <div class="table-toolbar">
                <h3>{{ selectedAdminCourse }}作业批次</h3>
                <button type="button">复制上一批次</button>
              </div>
              <div v-if="selectedAdminCourse === '物理'" class="form-grid">
                <label>作业名称<input value="物理暑期第 3 次作业" /></label>
                <label>所属课程<input value="物理 · 牛顿第二定律中的供需关系" /></label>
                <label>提交截止<input value="2026-07-18 22:00" /></label>
                <label>提分宝预计开放<input value="2026-07-20 18:00" /></label>
              </div>
              <div v-if="selectedAdminCourse === '物理'" class="upload-panel">
                <strong>答案文件：物理第3次答案.pdf</strong>
                <span>开放规则：学生提交该课程作业后自动开放</span>
                <button type="button">替换</button>
              </div>
              <AdminTable
                v-if="selectedAdminCourse === '物理'"
                :columns="questionColumns"
                :rows="questionRows"
              />
              <div v-else class="upload-panel large">
                <strong>{{ selectedAdminCourse }}作业由 AI 教学智能体小程序承接</strong>
                <span>学生端点击该课程作业时提示跳转 AI 小程序，不在本系统上传作业。</span>
                <button type="button">同步小程序任务</button>
              </div>
              <AdminTable :columns="aiTaskColumns" :rows="selectedAiTaskRows" />
            </section>

            <section v-else-if="activeCourseSubTab === 'scores'" class="admin-card">
              <div class="table-toolbar">
                <h3>{{ selectedAdminCourse }}小题分导出</h3>
                <div>
                  <button type="button" class="danger">下载异常清单</button>
                  <button type="button">导出学科网模板</button>
                </div>
              </div>
              <div class="ops-checklist">
                <span>导出范围：{{ selectedAdminCourse }} · 已提交小题分</span>
                <span>排除：未提交、超分、缺题</span>
                <span>导出后：学生端保持生成中</span>
              </div>
              <AdminTable :columns="scoreColumns" :rows="scoreRows" />
            </section>

            <section v-else class="admin-card">
              <h3>{{ selectedAdminCourse }}提分宝文件匹配与发布</h3>
              <div class="upload-panel large">
                <strong>拖入学科网返回的 {{ selectedAdminCourse }} 提分宝文件包</strong>
                <span>优先按学生编号、手机号、姓名匹配，待人工匹配文件不会发布。</span>
                <button type="button">选择文件</button>
              </div>
              <div class="ops-checklist">
                <span>匹配成功：可发布到学生端</span>
                <span>待人工匹配：不展示给学生</span>
                <span>发布后：对应课程提分宝入口显示新</span>
              </div>
              <AdminTable :columns="fileColumns" :rows="fileRows" />
              <div class="publish-row">
                <p>确认发布后，学生端状态将从“生成中”变为“可下载”。</p>
                <button type="button" @click="publishTifenbao">仅发布已匹配</button>
              </div>
            </section>

            <section class="admin-card publish-card">
              <span class="status-badge warning">课程内规则</span>
              <h3>学生端课程卡动作</h3>
              <ul>
                <li>去上课：跳转当前课程的小鹅通入口</li>
                <li>讲义：进入当前课程资料下载页</li>
                <li>作业：物理走本系统，三科跳 AI 小程序</li>
                <li>提分宝：按当前课程和作业状态展示</li>
              </ul>
              <button type="button">发布课程配置</button>
            </section>
          </template>
        </div>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onMounted, onUnmounted, reactive, ref } from "vue";
import { showToast } from "vant";
import successImage from "../studentAuth/assets/success.png";
import authQrImage from "../studentAuth/assets/qrcode.png";
import { gradePages, type GradeId } from "../gaokaoLive/data";
import "./styles.scss";

type StudentStep =
  | "login"
  | "verify"
  | "confirm"
  | "bindPhone"
  | "success"
  | "home"
  | "materials"
  | "tasks"
  | "submit"
  | "answer"
  | "score"
  | "waiting"
  | "download";
type AdminTab = "overview" | "students" | "courses";
type CourseSubTab = "profile" | "materials" | "tasks" | "scores" | "reports";
type PhoneBindMode = "wechat" | "manual";
type ResourceTarget = "materials" | "tasks" | "tifenbao";
type TableRow = Record<string, string>;

const AdminTable = defineComponent({
  name: "AdminTable",
  props: {
    columns: {
      type: Array as () => string[],
      required: true,
    },
    rows: {
      type: Array as () => TableRow[],
      required: true,
    },
  },
  setup(props) {
    return () =>
      h("div", { class: "admin-table", style: { "--columns": String(props.columns.length) } }, [
        h("div", { class: "admin-table__row admin-table__head" }, props.columns.map((column) => h("span", column))),
        ...props.rows.map((row) =>
          h("div", { class: "admin-table__row" }, props.columns.map((column) => h("span", row[column] ?? "-"))),
        ),
      ]);
  },
});

const fullFlowNodes = ["登录验证", "注册认证", "绑定手机号", "学生主页", "课程学习", "讲义下载", "作业提交", "等待生成", "下载提分宝"];
const activeMode = ref<"student" | "admin">("student");
const studentStep = ref<StudentStep>("login");
const activeAdminTab = ref<AdminTab>("overview");
const activeCourseSubTab = ref<CourseSubTab>("profile");
const phoneBindMode = ref<PhoneBindMode>("manual");
const wechatPhoneCaptured = ref(false);
const phoneVerificationCode = ref("");
const verificationCodeSent = ref(false);
const smsCountdown = ref(0);
const phoneSyncedToStudent = ref(false);
const activeGradeId = ref<GradeId>("senior-three");
const selectedSubject = ref("物理");
const selectedAdminCourse = ref("");
const isCreatingCourse = ref(false);
const courseImportFetched = ref(false);
const hasDownloaded = ref(false);
const materialDownloaded = ref(false);
const homeworkSubmitted = ref(false);
const homeworkSubmittedLate = ref(false);
const homeworkSubmittedAt = ref("");
const scoreSubmitted = ref(false);
const tifenbaoPublished = ref(false);
const isCertified = ref(true);
const uploadedFiles = ref(["第1页", "第2页"]);
const scoreError = ref("");
const physicsHomeworkDeadline = "2026-07-18 22:00";
const physicsHomeworkDeadlineValue = "2026-07-18T22:00:00+08:00";
const currentTime = ref(new Date());
let currentTimeTimer: number | undefined;
let smsCountdownTimer: number | undefined;

const courseSchedules: Record<string, Array<{ startTime: string; endTime: string }>> = {
  数学: [
    { startTime: "2026-07-01T09:00:00+08:00", endTime: "2026-07-01T11:00:00+08:00" },
    { startTime: "2026-07-03T09:00:00+08:00", endTime: "2026-07-03T11:00:00+08:00" },
  ],
  物理: [
    { startTime: "2026-07-02T14:00:00+08:00", endTime: "2026-07-02T16:00:00+08:00" },
    { startTime: "2026-07-04T14:00:00+08:00", endTime: "2026-07-04T16:00:00+08:00" },
  ],
  英语: [
    { startTime: "2026-07-06T19:00:00+08:00", endTime: "2026-07-06T21:00:00+08:00" },
  ],
  语文: [
    { startTime: "2026-07-07T09:00:00+08:00", endTime: "2026-07-07T11:00:00+08:00" },
  ],
};

const materialCatalog: Record<string, Array<{
  title: string;
  meta: string;
  displayTime: string;
}>> = {
  数学: [
    {
      title: "数学暑期课程配套讲义.pdf",
      meta: "PDF · 函数专题讲义",
      displayTime: "2026-06-20T08:00:00+08:00",
    },
  ],
  物理: [
    {
      title: "物理暑期课程配套讲义.pdf",
      meta: "PDF · 牛顿第二定律课程讲义",
      displayTime: "2026-06-20T08:00:00+08:00",
    },
    {
      title: "物理第3次作业讲义.pdf",
      meta: "PDF · 作业配套讲义 · 提交作业前建议下载",
      displayTime: "2026-07-01T20:00:00+08:00",
    },
  ],
  英语: [
    {
      title: "英语暑期课程配套讲义.pdf",
      meta: "PDF · 阅读能力训练",
      displayTime: "2026-06-24T08:00:00+08:00",
    },
  ],
  语文: [
    {
      title: "语文暑期课程配套讲义.pdf",
      meta: "PDF · 古代诗歌鉴赏",
      displayTime: "2026-07-05T08:00:00+08:00",
    },
  ],
};

function formatBusinessTime(value: string) {
  return new Intl.DateTimeFormat("zh-CN", {
    timeZone: "Asia/Shanghai",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(value)).replace(/\//g, "-");
}

function resolveNextLessonTime(subject: string) {
  const nextLesson = (courseSchedules[subject] ?? [])
    .filter((lesson) => new Date(lesson.startTime).getTime() > currentTime.value.getTime())
    .sort(
      (left, right) =>
        new Date(left.startTime).getTime() - new Date(right.startTime).getTime(),
    )[0];

  if (!nextLesson) return "暂无未开始课次";
  return `${formatBusinessTime(nextLesson.startTime)}-${formatBusinessTime(nextLesson.endTime).slice(-5)}`;
}

const isHomeworkOverdue = computed(
  () => currentTime.value.getTime() > new Date(physicsHomeworkDeadlineValue).getTime(),
);

onMounted(() => {
  currentTimeTimer = window.setInterval(() => {
    currentTime.value = new Date();
  }, 60_000);
});

onUnmounted(() => {
  if (currentTimeTimer !== undefined) window.clearInterval(currentTimeTimer);
  if (smsCountdownTimer !== undefined) window.clearInterval(smsCountdownTimer);
});

const courseImportForm = reactive({
  url: "https://jxea4.xetslk.com/s/physics-summer-2026",
});

const loginForm = reactive({
  phone: "17609090909",
  name: "慧小聚",
});

const authForm = reactive({
  school: "杭州第二中学",
  grade: "高三年级",
  name: "慧小聚",
  studentId: "20260018",
  phone: "17609090909",
});

const scoreItems = reactive([
  { no: 1, score: 8, full: 10 },
  { no: 2, score: 10, full: 10 },
  { no: 3, score: 7.5, full: 12 },
  { no: 4, score: 0, full: 8 },
  { no: 5, score: 12, full: 15 },
]);

const studentSteps: Array<{ key: StudentStep; label: string; index: string }> = [
  { key: "login", label: "登录验证", index: "01" },
  { key: "verify", label: "注册认证", index: "02" },
  { key: "confirm", label: "确认报名", index: "03" },
  { key: "bindPhone", label: "绑定手机号", index: "04" },
  { key: "success", label: "开通成功", index: "05" },
  { key: "home", label: "学生主页", index: "06" },
  { key: "materials", label: "讲义资料", index: "07" },
  { key: "tasks", label: "作业任务", index: "08" },
  { key: "submit", label: "提交作业", index: "09" },
  { key: "answer", label: "答案开放", index: "10" },
  { key: "score", label: "小题分", index: "11" },
  { key: "waiting", label: "等待期", index: "12" },
  { key: "download", label: "下载", index: "13" },
];

const adminTabs: Array<{ key: AdminTab; label: string }> = [
  { key: "overview", label: "运营总览" },
  { key: "students", label: "学员权限" },
  { key: "courses", label: "课程管理" },
];

const activeGradePage = computed(() => gradePages.find((page) => page.id === activeGradeId.value) ?? gradePages[0]);
const currentStudentStep = computed(() => studentSteps.find((step) => step.key === studentStep.value));
const maskedPhone = computed(() => `${authForm.phone.slice(0, 3)}****${authForm.phone.slice(-4)}`);
const canVerifyPhone = computed(
  () =>
    /^1\d{10}$/.test(authForm.phone) &&
    verificationCodeSent.value &&
    /^\d{6}$/.test(phoneVerificationCode.value),
);
const tifenbaoStatus = computed(() => {
  if (homeworkSubmittedLate.value) return "未生成";
  if (hasDownloaded.value) return "已下载";
  if (tifenbaoPublished.value) return "可下载";
  if (scoreSubmitted.value) return "生成中";
  return "未生成";
});

const staticSubjectState: Record<string, {
  materialStatus: string;
  homeworkStatus: string;
  tifenbaoStatus: string;
  homeworkTitle: string;
  homeworkDesc: string;
  taskAction: StudentStep;
  taskButton: string;
  taskTone: string;
  materialCount: string;
  reportTitle: string;
  tifenbaoHint: string;
  timelineStart: string;
  exportStatus: string;
  tone: string;
  hasNewMaterial: boolean;
  hasNewHomework: boolean;
  hasNewTifenbao: boolean;
}> = {
  数学: {
    materialStatus: "已下载",
    homeworkStatus: "去 AI 小程序",
    tifenbaoStatus: "生成中",
    homeworkTitle: "数学函数专题练习",
    homeworkDesc: "数学作业请前往 AI 教学智能体小程序完成提交与智批，本系统同步展示提分宝生成进度。",
    taskAction: "tasks",
    taskButton: "去 AI 小程序",
    taskTone: "blue",
    materialCount: "1 份讲义",
    reportTitle: "数学函数专题练习",
    tifenbaoHint: "数学作业已由 AI 教学智能体完成批改，小题分已导出给学科网，预计 07-20 18:00 开放下载。",
    timelineStart: "07-18 20:10",
    exportStatus: "已导出",
    tone: "blue",
    hasNewMaterial: false,
    hasNewHomework: false,
    hasNewTifenbao: true,
  },
  英语: {
    materialStatus: "已下载",
    homeworkStatus: "去 AI 小程序",
    tifenbaoStatus: "可下载",
    homeworkTitle: "英语阅读能力训练",
    homeworkDesc: "英语作业请前往 AI 教学智能体小程序完成提交与智批，本系统已同步提分宝文件。",
    taskAction: "tasks",
    taskButton: "去 AI 小程序",
    taskTone: "green",
    materialCount: "1 份讲义",
    reportTitle: "英语阅读能力训练",
    tifenbaoHint: "英语作业提分宝已发布，可直接下载。",
    timelineStart: "07-18 19:42",
    exportStatus: "已导出",
    tone: "green",
    hasNewMaterial: false,
    hasNewHomework: false,
    hasNewTifenbao: true,
  },
  语文: {
    materialStatus: "待下载",
    homeworkStatus: "去 AI 小程序",
    tifenbaoStatus: "未生成",
    homeworkTitle: "语文诗歌鉴赏训练",
    homeworkDesc: "语文作业请前往 AI 教学智能体小程序查看开放状态，完成后本系统同步提分宝进度。",
    taskAction: "tasks",
    taskButton: "去 AI 小程序",
    taskTone: "gray",
    materialCount: "1 份讲义",
    reportTitle: "语文诗歌鉴赏训练",
    tifenbaoHint: "语文作业尚未开放，完成 AI 智批后才会进入提分宝生成流程。",
    timelineStart: "未开始",
    exportStatus: "未开始",
    tone: "gray",
    hasNewMaterial: true,
    hasNewHomework: false,
    hasNewTifenbao: false,
  },
};

const subjectResources = computed(() =>
  activeGradePage.value.courses.map((course) => {
    if (course.subject === "物理") {
      const status = homeworkSubmittedLate.value
        ? "超时提交"
        : scoreSubmitted.value || tifenbaoStatus.value === "可下载" || tifenbaoStatus.value === "已下载"
          ? "全部完成"
          : homeworkSubmitted.value
            ? "待录分"
            : "待提交";
      return {
        subject: course.subject,
        shortName: course.subject.slice(0, 1),
        teacher: course.teacher,
        topic: course.topics[0],
        lessonTime: `${course.date} ${course.times.join(" / ")}`,
        nextLessonTime: resolveNextLessonTime(course.subject),
        materialStatus: materialDownloaded.value ? "已下载" : "待下载",
        materialCount: "2 份讲义",
        homeworkTitle: "物理暑期第 3 次作业",
        homeworkDesc: "物理作业在本系统内完成拍照上传，截止前提交后直接进入提分宝生成等待期。",
        homeworkStatus: status,
        taskAction: tifenbaoStatus.value === "可下载" || tifenbaoStatus.value === "已下载"
          ? "download"
          : homeworkSubmittedLate.value
            ? "answer"
            : scoreSubmitted.value
            ? "waiting"
            : homeworkSubmitted.value
              ? "score"
              : "submit",
        taskButton: tifenbaoStatus.value === "可下载" || tifenbaoStatus.value === "已下载"
          ? "下载提分宝"
          : homeworkSubmittedLate.value
            ? "查看答案"
            : scoreSubmitted.value
            ? "查看进度"
            : homeworkSubmitted.value
              ? "填写小题分"
              : "提交作业",
        taskTone: tifenbaoStatus.value === "可下载" || tifenbaoStatus.value === "已下载"
          ? "green"
          : homeworkSubmittedLate.value
            ? "gray"
            : scoreSubmitted.value
            ? "blue"
            : "orange",
        tifenbaoStatus: tifenbaoStatus.value,
        reportTitle: "物理暑期第3次作业",
        answerTitle: "物理第3次作业答案.pdf",
        tifenbaoHint: homeworkSubmittedLate.value
          ? "本次物理作业超出截止时间提交，不进入小题分录入和提分宝生成流程。"
          : "已收到你的物理作业，系统正在整理数据并生成提分宝。预计开放时间：07-20 18:00。",
        timelineStart: homeworkSubmittedAt.value || (scoreSubmitted.value ? "07-17 21:06" : "待提交"),
        exportStatus: homeworkSubmittedLate.value ? "不生成" : scoreSubmitted.value ? "处理中" : "未开始",
        tone: "orange",
        hasNewMaterial: !materialDownloaded.value,
        hasNewHomework: !scoreSubmitted.value && !homeworkSubmittedLate.value,
        hasNewTifenbao: tifenbaoStatus.value === "生成中" || tifenbaoStatus.value === "可下载",
      };
    }

    const state = staticSubjectState[course.subject] ?? staticSubjectState.语文;
    return {
      subject: course.subject,
      shortName: course.subject.slice(0, 1),
      teacher: course.teacher,
      topic: course.topics[0],
      lessonTime: `${course.date} ${course.times.join(" / ")}`,
      nextLessonTime: resolveNextLessonTime(course.subject),
      ...state,
      answerTitle: `${course.subject}作业答案.pdf`,
    };
  }),
);

const selectedSubjectResource = computed(() =>
  subjectResources.value.find((resource) => resource.subject === selectedSubject.value) ?? subjectResources.value[0],
);

const selectedMaterialFiles = computed(() => {
  return (materialCatalog[selectedSubjectResource.value.subject] ?? [])
    .filter(
      (file) => currentTime.value.getTime() >= new Date(file.displayTime).getTime(),
    )
    .map((file) => ({
      title: file.title,
      meta: `${file.meta} · ${formatBusinessTime(file.displayTime)} 起显示`,
    }));
});

const selectedTaskCards = computed(() =>
  taskCards.value.filter((task) => task.fullSubject === selectedSubjectResource.value.subject),
);
const selectedCourseHomeCard = computed(() =>
  subjectResources.value.find((resource) => resource.subject === selectedAdminCourse.value) ?? subjectResources.value[0],
);
const selectedCourseCardEntries = computed(() => {
  const card = selectedCourseHomeCard.value;
  return [
    {
      name: "讲义",
      status: card.materialStatus,
      hasNew: card.hasNewMaterial,
      source: "课程资料发布状态",
      action: "进入当前课程资料下载页",
    },
    {
      name: "作业",
      status: card.homeworkStatus,
      hasNew: card.hasNewHomework,
      source: card.subject === "物理" ? "本系统作业批次" : "AI 教学智能体小程序",
      action: card.subject === "物理" ? "进入上传/答案/录分流程" : "提示跳转 AI 小程序",
    },
    {
      name: "提分宝",
      status: card.tifenbaoStatus,
      hasNew: card.hasNewTifenbao,
      source: "提分宝文件发布状态",
      action: "按未生成/生成中/可下载展示",
    },
  ];
});
const courseCardEntryColumns = ["入口", "学生端文案", "状态来源", "新提示", "点击动作"];
const selectedCourseCardEntryRows = computed(() =>
  selectedCourseCardEntries.value.map((entry) => ({
    入口: entry.name,
    学生端文案: entry.status,
    状态来源: entry.source,
    新提示: entry.hasNew ? "显示新" : "不显示",
    点击动作: entry.action,
  })),
);
const importedCourseCard = computed(() => ({
  subject: "物理",
  topic: "高三:牛顿第二定律中的供需关系",
  teacher: "王天宇",
  nextLessonTime: "6月7日 19:00-20:00",
  entryStatus: "小鹅通课程链接已绑定",
}));
const importedCourseCardEntries = computed(() => [
  {
    name: "讲义",
    status: "待配置",
    hasNew: false,
    source: "课程创建后上传讲义",
    action: "进入当前课程资料下载页",
  },
  {
    name: "作业",
    status: "待配置",
    hasNew: false,
    source: importedCourseCard.value.subject === "物理" ? "本系统作业批次" : "AI 教学智能体小程序",
    action: importedCourseCard.value.subject === "物理" ? "进入上传/答案/录分流程" : "提示跳转 AI 小程序",
  },
  {
    name: "提分宝",
    status: "未生成",
    hasNew: false,
    source: "提分宝文件发布状态",
    action: "按未生成/生成中/可下载展示",
  },
]);
const importedCourseCardEntryRows = computed(() =>
  importedCourseCardEntries.value.map((entry) => ({
    入口: entry.name,
    学生端文案: entry.status,
    状态来源: entry.source,
    新提示: entry.hasNew ? "显示新" : "不显示",
    点击动作: entry.action,
  })),
);

const subjectTodoItems = computed(() =>
  subjectResources.value.flatMap((resource) => {
    const items: Array<{ subject: string; title: string; desc: string; tone: string }> = [];
    if (resource.materialStatus === "待下载") {
      items.push({
        subject: resource.subject,
        title: "讲义资料待下载",
        desc: `${resource.topic} · ${resource.materialCount}`,
        tone: "info",
      });
    }
    if (resource.homeworkStatus === "待提交") {
      items.push({
        subject: resource.subject,
        title: "作业任务未提交",
        desc: `${resource.homeworkTitle} 截止 ${physicsHomeworkDeadline}`,
        tone: "warning",
      });
    }
    if (resource.homeworkStatus === "超时提交") {
      items.push({
        subject: resource.subject,
        title: "作业已超时提交",
        desc: "无法录入小题分，本次不生成提分宝",
        tone: "warning",
      });
    }
    if (resource.tifenbaoStatus === "生成中") {
      items.push({
        subject: resource.subject,
        title: "提分宝生成中",
        desc: "预计 07-20 18:00 开放下载，当前等待学科网生成",
        tone: "progress",
      });
    }
    if (resource.tifenbaoStatus === "可下载") {
      items.push({
        subject: resource.subject,
        title: "提分宝可下载",
        desc: `${resource.reportTitle}提分宝已发布`,
        tone: "ready",
      });
    }
    return items;
  }),
);

const taskCards = computed(() => [
  {
    subject: "物",
    fullSubject: "物理",
    title: "物理暑期第 3 次作业",
    desc: "提交截止 07-18 22:00 · 预计开放 07-20 18:00",
    status: tifenbaoStatus.value === "可下载" || tifenbaoStatus.value === "已下载"
      ? tifenbaoStatus.value
      : homeworkSubmittedLate.value
        ? "超时提交"
        : scoreSubmitted.value
        ? "生成中"
        : homeworkSubmitted.value
          ? "待录分"
          : "待提交",
    button: tifenbaoStatus.value === "可下载" || tifenbaoStatus.value === "已下载"
      ? "下载提分宝"
      : homeworkSubmittedLate.value
        ? "查看答案"
        : scoreSubmitted.value
        ? "查看进度"
        : homeworkSubmitted.value
          ? "填写小题分"
          : "提交作业",
    action: tifenbaoStatus.value === "可下载" || tifenbaoStatus.value === "已下载"
      ? "download"
      : homeworkSubmittedLate.value
        ? "answer"
        : scoreSubmitted.value
        ? "waiting"
        : homeworkSubmitted.value
          ? "score"
          : "submit",
    tone: tifenbaoStatus.value === "可下载" || tifenbaoStatus.value === "已下载"
      ? "green"
      : homeworkSubmittedLate.value
        ? "gray"
        : scoreSubmitted.value
        ? "blue"
        : "orange",
  },
  {
    subject: "数",
    fullSubject: "数学",
    title: "数学函数专题练习",
    desc: "作业提交与智批在 AI 教学智能体小程序完成",
    status: "需前往 AI 小程序",
    button: "去 AI 小程序",
    action: "aiAgent",
    tone: "blue",
  },
  {
    subject: "英",
    fullSubject: "英语",
    title: "英语阅读能力训练",
    desc: "作业提交与智批在 AI 教学智能体小程序完成",
    status: "需前往 AI 小程序",
    button: "去 AI 小程序",
    action: "aiAgent",
    tone: "green",
  },
  {
    subject: "语",
    fullSubject: "语文",
    title: "语文诗歌鉴赏训练",
    desc: "作业开放与智批在 AI 教学智能体小程序查看",
    status: "需前往 AI 小程序",
    button: "去 AI 小程序",
    action: "aiAgent",
    tone: "gray",
  },
]);

const scoreTotal = computed(() => scoreItems.reduce((sum, item) => sum + Number(item.score || 0), 0));
const fullTotal = computed(() => scoreItems.reduce((sum, item) => sum + item.full, 0));
const activeAdminLabel = computed(() => adminTabs.find((tab) => tab.key === activeAdminTab.value)?.label ?? "");
const studentFilters = reactive({
  school: "",
  grade: "",
  className: "",
  name: "",
});

const overviewMetrics = [
  { label: "已认证学员", value: "186", desc: "176 人已绑定手机号", tone: "blue" },
  { label: "课程权限", value: "744", desc: "4 个学科入口已开通", tone: "green" },
  { label: "物理作业", value: "142/186", desc: "44 人待提交", tone: "orange" },
  { label: "提分宝", value: "1 科可下载", desc: "2 科生成中 / 1 科未生成", tone: "gray" },
];

const opsFlowNodes = [
  { step: "01", title: "学员权限", desc: "导入名单，校验手机号、姓名和已开通课程", tone: "blue" },
  { step: "02", title: "课程管理", desc: "在课程内配置上课入口、讲义、作业和提分宝", tone: "green" },
  { step: "03", title: "课程作业", desc: "物理走本系统，数学/语文/英语跳 AI 小程序", tone: "orange" },
  { step: "04", title: "课程提分宝", desc: "基于课程导出小题分、匹配文件并发布", tone: "purple" },
];

const todoColumns = ["事项", "学科", "影响学生", "截止/开放", "处理动作"];
const todoRows = [
  { 事项: "物理作业未提交提醒", 学科: "物理", 影响学生: "44 人", "截止/开放": "07-18 22:00", 处理动作: "发送提醒" },
  { 事项: "数学提分宝生成中", 学科: "数学", 影响学生: "186 人", "截止/开放": "07-20 18:00", 处理动作: "跟进学科网" },
  { 事项: "语文讲义待下载", 学科: "语文", 影响学生: "62 人", "截止/开放": "已发布", 处理动作: "推送下载提醒" },
  { 事项: "英语提分宝已可下载", 学科: "英语", 影响学生: "186 人", "截止/开放": "08-20 失效", 处理动作: "查看下载率" },
];

const courseMetrics = [
  { label: "课程学科", value: "4", desc: "数学 / 物理 / 英语 / 语文", tone: "blue" },
  { label: "讲义资料", value: "5", desc: "3 份已发布 / 2 份待下载提醒", tone: "green" },
  { label: "课程作业", value: "4", desc: "3 个 AI 小程序任务 / 1 个物理批次", tone: "orange" },
  { label: "提分宝", value: "3 状态", desc: "未生成 / 生成中 / 可下载", tone: "gray" },
];
const courseSubTabs: Array<{ key: CourseSubTab; label: string }> = [
  { key: "profile", label: "学生端卡片" },
  { key: "materials", label: "课程资料" },
  { key: "tasks", label: "作业批次" },
  { key: "scores", label: "小题分导出" },
  { key: "reports", label: "提分宝发布" },
];

const studentColumns = ["学校", "年级", "班级", "姓名", "手机号", "授权课程", "权限状态", "操作"];
const studentRows = [
  { 学校: "杭州一中", 年级: "高三", 班级: "暑期 A 班", 姓名: "慧小聚", 手机号: "176****0909", 授权课程: "数学 / 物理 / 英语 / 语文", 权限状态: "已认证可登录", 操作: "编辑 / 删除" },
  { 学校: "杭州一中", 年级: "高三", 班级: "暑期 A 班", 姓名: "陈同学", 手机号: "138****2331", 授权课程: "数学 / 物理 / 英语", 权限状态: "已认证可登录", 操作: "编辑 / 删除" },
  { 学校: "宁波二中", 年级: "高三", 班级: "暑期 B 班", 姓名: "林同学", 手机号: "-", 授权课程: "物理", 权限状态: "待补手机号", 操作: "编辑 / 删除" },
  { 学校: "温州实验", 年级: "高二", 班级: "暑期 C 班", 姓名: "周同学", 手机号: "177****0021", 授权课程: "数学 / 物理 / 英语 / 语文", 权限状态: "待注册认证", 操作: "编辑 / 删除" },
  { 学校: "杭州一中", 年级: "高二", 班级: "暑期 C 班", 姓名: "许同学", 手机号: "-", 授权课程: "数学 / 英语", 权限状态: "名单已导入", 操作: "编辑 / 删除" },
];
const studentFilterOptions = {
  schools: Array.from(new Set(studentRows.map((row) => row.学校))),
  grades: Array.from(new Set(studentRows.map((row) => row.年级))),
  classes: Array.from(new Set(studentRows.map((row) => row.班级))),
};
const filteredStudentRows = computed(() =>
  studentRows.filter((row) => {
    const matchesSchool = !studentFilters.school || row.学校 === studentFilters.school;
    const matchesGrade = !studentFilters.grade || row.年级 === studentFilters.grade;
    const matchesClass = !studentFilters.className || row.班级 === studentFilters.className;
    const matchesName = !studentFilters.name || row.姓名.includes(studentFilters.name);
    return matchesSchool && matchesGrade && matchesClass && matchesName;
  }),
);

const courseColumns = ["学科", "上课入口", "讲义资料", "作业入口", "提分宝状态", "学生端展示"];
const courseRows = [
  { 学科: "数学", 上课入口: "小鹅通已绑定", 讲义资料: "1 份已发布", 作业入口: "AI 小程序", 提分宝状态: "生成中", 学生端展示: "有新提分宝" },
  { 学科: "物理", 上课入口: "小鹅通已绑定", 讲义资料: "2 份待下载", 作业入口: "本系统上传", 提分宝状态: "未生成", 学生端展示: "讲义新/作业新" },
  { 学科: "英语", 上课入口: "小鹅通已绑定", 讲义资料: "1 份已发布", 作业入口: "AI 小程序", 提分宝状态: "可下载", 学生端展示: "有新提分宝" },
  { 学科: "语文", 上课入口: "小鹅通已绑定", 讲义资料: "1 份待下载", 作业入口: "AI 小程序", 提分宝状态: "未生成", 学生端展示: "讲义新" },
];
const selectedAdminCourseRow = computed(() => courseRows.find((row) => row.学科 === selectedAdminCourse.value));

const materialColumns = ["资料名称", "所属学科", "绑定课程", "学生端状态", "发布动作"];
const materialRows = [
  { 资料名称: "数学暑期课程配套讲义.pdf", 所属学科: "数学", 绑定课程: "函数的对称性", 学生端状态: "已下载", 发布动作: "替换" },
  { 资料名称: "物理暑期课程配套讲义.pdf", 所属学科: "物理", 绑定课程: "牛顿第二定律", 学生端状态: "新/待下载", 发布动作: "下架" },
  { 资料名称: "物理第3次作业讲义.pdf", 所属学科: "物理", 绑定课程: "物理第3次作业", 学生端状态: "新/待下载", 发布动作: "替换" },
  { 资料名称: "语文诗歌鉴赏讲义.pdf", 所属学科: "语文", 绑定课程: "古代诗歌鉴赏", 学生端状态: "新/待下载", 发布动作: "提醒" },
];
const selectedMaterialRows = computed(() => materialRows.filter((row) => row.所属学科 === selectedAdminCourse.value));

const questionColumns = ["题号", "满分", "必填", "规则"];
const questionRows = [
  { 题号: "1", 满分: "10", 必填: "是", 规则: "允许 0.5" },
  { 题号: "2", 满分: "10", 必填: "是", 规则: "整数" },
  { 题号: "3", 满分: "12", 必填: "是", 规则: "允许 0.5" },
  { 题号: "4", 满分: "8", 必填: "是", 规则: "整数" },
];

const aiTaskColumns = ["学科", "小程序任务", "批改方式", "小题分来源", "学生端动作"];
const aiTaskRows = [
  { 学科: "数学", 小程序任务: "数学函数专题练习", 批改方式: "AI 教学智能体智批", 小题分来源: "后台导出", 学生端动作: "提示去 AI 小程序" },
  { 学科: "英语", 小程序任务: "英语阅读能力训练", 批改方式: "AI 教学智能体智批", 小题分来源: "后台导出", 学生端动作: "提示去 AI 小程序" },
  { 学科: "语文", 小程序任务: "语文诗歌鉴赏训练", 批改方式: "AI 教学智能体智批", 小题分来源: "后台导出", 学生端动作: "提示去 AI 小程序" },
  { 学科: "物理", 小程序任务: "-", 批改方式: "学生自批", 小题分来源: "学生填写", 学生端动作: "进入上传/答案/录分" },
];
const selectedAiTaskRows = computed(() => aiTaskRows.filter((row) => row.学科 === selectedAdminCourse.value));

const scoreMetrics = [
  { label: "作业提交", value: "142/186", desc: "44 人未提交", tone: "orange" },
  { label: "小题分已交", value: "128", desc: "可导出 126 人", tone: "blue" },
  { label: "异常数据", value: "2", desc: "超分或缺题", tone: "red" },
  { label: "生成中", value: "0", desc: "等待导出后开始", tone: "gray" },
];

const fileMetrics = [
  { label: "已上传文件", value: "126", desc: "本批次文件", tone: "blue" },
  { label: "自动匹配成功", value: "123", desc: "可直接发布", tone: "green" },
  { label: "待人工匹配", value: "3", desc: "需要处理", tone: "orange" },
  { label: "已发布", value: "0", desc: "发布后学生可下载", tone: "gray" },
];

const scoreColumns = ["学生", "手机号", "总分", "录分状态", "导出状态", "操作"];
const scoreRows = [
  { 学生: "慧小聚", 手机号: "176****0909", 总分: "72.5", 录分状态: "已提交", 导出状态: "待导出", 操作: "查看" },
  { 学生: "陈同学", 手机号: "138****2331", 总分: "68", 录分状态: "已提交", 导出状态: "待导出", 操作: "查看" },
  { 学生: "林同学", 手机号: "139****9871", 总分: "101", 录分状态: "异常", 导出状态: "不可导出", 操作: "退回" },
  { 学生: "周同学", 手机号: "177****0021", 总分: "-", 录分状态: "未提交", 导出状态: "不导出", 操作: "提醒" },
];

const fileColumns = ["文件名", "匹配学生", "匹配方式", "状态", "操作"];
const fileRows = [
  { 文件名: "物理3_1760909_慧小聚.pdf", 匹配学生: "慧小聚", 匹配方式: "手机号+姓名", 状态: "可发布", 操作: "预览" },
  { 文件名: "物理3_陈同学.pdf", 匹配学生: "陈同学", 匹配方式: "姓名+班级", 状态: "待确认", 操作: "匹配" },
  { 文件名: "phy_03_1399871.pdf", 匹配学生: "林同学", 匹配方式: "手机号", 状态: "可发布", 操作: "预览" },
  { 文件名: "未识别_003.pdf", 匹配学生: "-", 匹配方式: "人工处理", 状态: "待匹配", 操作: "选择学生" },
];

function goStep(step: StudentStep) {
  activeMode.value = "student";
  if (["submit", "answer", "score", "waiting", "download"].includes(step)) {
    selectedSubject.value = "物理";
  }
  studentStep.value = step;
}

function openCourseLesson(subject: string) {
  showToast(`${subject}课程请跳转小鹅通上课`);
}

function goSubjectResource(subject: string, target: ResourceTarget) {
  selectedSubject.value = subject;
  if (target === "materials") {
    goStep("materials");
    return;
  }
  if (target === "tasks") {
    if (subject !== "物理") {
      openAiAgentMiniProgram(subject);
      return;
    }
    if (tifenbaoStatus.value === "可下载" || tifenbaoStatus.value === "已下载") {
      goStep("download");
      return;
    }
    if (scoreSubmitted.value) {
      goStep("waiting");
      return;
    }
    if (homeworkSubmitted.value) {
      goStep("answer");
      return;
    }
    goStep("submit");
    return;
  }
  const resource = subjectResources.value.find((item) => item.subject === subject);
  if (resource?.tifenbaoStatus === "可下载" || resource?.tifenbaoStatus === "已下载") {
    goStep("download");
    return;
  }
  goStep(resource?.tifenbaoStatus === "生成中" ? "waiting" : "tasks");
}

function openAiAgentMiniProgram(subject: string) {
  showToast(`${subject}作业请跳转 AI 教学智能体小程序完成`);
}

function loginStudent() {
  if (!/^1\d{10}$/.test(loginForm.phone)) {
    showToast("请输入正确手机号");
    return;
  }
  if (!loginForm.name.trim()) {
    showToast("请输入学生姓名");
    return;
  }
  const isMatched = loginForm.phone === authForm.phone && loginForm.name.trim() === authForm.name;
  if (!isCertified.value || !isMatched) {
    showToast("未找到认证信息，请先注册认证");
    return;
  }
  showToast("登录成功");
  goStep("home");
}

function verifyStudent() {
  if (!authForm.school || !authForm.grade || !authForm.name || !authForm.studentId) {
    showToast("请先补全认证信息");
    return;
  }
  if (isCertified.value) {
    showToast("您已认证，下次可以直接登录");
    goStep("home");
    return;
  }
  goStep("confirm");
}

function bindPhone() {
  if (!/^1\d{10}$/.test(authForm.phone)) {
    showToast("请输入正确手机号");
    return;
  }
  if (phoneBindMode.value === "manual") {
    if (!verificationCodeSent.value) {
      showToast("请先获取短信验证码");
      return;
    }
    if (phoneVerificationCode.value !== "123456") {
      showToast("验证码错误或已过期");
      return;
    }
  }
  isCertified.value = true;
  phoneSyncedToStudent.value = true;
  loginForm.phone = authForm.phone;
  loginForm.name = authForm.name;
  showToast("手机号验证成功，已同步至学员信息");
  goStep("success");
}

function sendPhoneVerificationCode() {
  if (!/^1\d{10}$/.test(authForm.phone)) {
    showToast("请输入正确手机号");
    return;
  }
  verificationCodeSent.value = true;
  phoneVerificationCode.value = "";
  smsCountdown.value = 60;
  if (smsCountdownTimer !== undefined) window.clearInterval(smsCountdownTimer);
  smsCountdownTimer = window.setInterval(() => {
    smsCountdown.value -= 1;
    if (smsCountdown.value <= 0 && smsCountdownTimer !== undefined) {
      window.clearInterval(smsCountdownTimer);
      smsCountdownTimer = undefined;
    }
  }, 1_000);
  showToast("验证码已发送");
}

function getWechatPhone() {
  authForm.phone = "17609090909";
  wechatPhoneCaptured.value = true;
  phoneSyncedToStudent.value = false;
  showToast("已模拟获取微信绑定手机号");
}

function switchPhoneBindMode(mode: PhoneBindMode) {
  phoneBindMode.value = mode;
  wechatPhoneCaptured.value = mode === "wechat" ? wechatPhoneCaptured.value : false;
  phoneVerificationCode.value = "";
  verificationCodeSent.value = false;
  smsCountdown.value = 0;
  if (smsCountdownTimer !== undefined) {
    window.clearInterval(smsCountdownTimer);
    smsCountdownTimer = undefined;
  }
}

function handleTaskAction(action: string) {
  if (action === "submit") goStep("submit");
  if (action === "score") {
    if (homeworkSubmittedLate.value) {
      showToast("超时提交，无法录入小题分");
      goStep("answer");
      return;
    }
    goStep("score");
  }
  if (action === "waiting") goStep("waiting");
  if (action === "download") goStep("download");
  if (action === "answer") goStep("answer");
  if (action === "aiAgent") openAiAgentMiniProgram(selectedSubject.value);
  if (action === "tasks") showToast("该学科作业批次暂未开放");
}

function resetStudentFilters() {
  studentFilters.school = "";
  studentFilters.grade = "";
  studentFilters.className = "";
  studentFilters.name = "";
}

function openAdminCourse(subject: string) {
  isCreatingCourse.value = false;
  selectedAdminCourse.value = subject;
  activeCourseSubTab.value = "profile";
}

function backToCourseList() {
  selectedAdminCourse.value = "";
  activeCourseSubTab.value = "profile";
}

function startCreateCourse() {
  selectedAdminCourse.value = "";
  isCreatingCourse.value = true;
  courseImportFetched.value = false;
}

function cancelCreateCourse() {
  isCreatingCourse.value = false;
  courseImportFetched.value = false;
}

function fetchXetCourseInfo() {
  if (!courseImportForm.url.trim()) {
    showToast("请先填写小鹅通课程链接");
    return;
  }
  courseImportFetched.value = true;
  showToast("已从小鹅通链接获取课程信息");
}

function addMockFile() {
  uploadedFiles.value.push(`第${uploadedFiles.value.length + 1}页`);
}

function submitHomework() {
  if (!uploadedFiles.value.length) {
    showToast("请先上传作业文件");
    return;
  }
  if (isHomeworkOverdue.value) {
    showToast("已超过作业提交截止时间");
    return;
  }
  selectedSubject.value = "物理";
  homeworkSubmittedLate.value = false;
  homeworkSubmittedAt.value = formatBusinessTime(currentTime.value.toISOString());
  homeworkSubmitted.value = true;
  scoreSubmitted.value = true;
  showToast("作业提交成功，正在生成提分宝");
  goStep("waiting");
}

function saveDraft() {
  if (homeworkSubmittedLate.value) {
    showToast("超时提交，无法保存小题分");
    return;
  }
  showToast("小题分草稿已保存");
}

function submitScore() {
  if (homeworkSubmittedLate.value) {
    showToast("超时提交，无法录入小题分");
    return;
  }
  const invalid = scoreItems.find((item) => Number(item.score) < 0 || Number(item.score) > item.full);
  if (invalid) {
    scoreError.value = `第 ${invalid.no} 题得分不能超过满分`;
    return;
  }
  scoreError.value = "";
  scoreSubmitted.value = true;
  goStep("waiting");
}

function publishTifenbao() {
  tifenbaoPublished.value = true;
  goStep("download");
  showToast("提分宝已发布，学生端可下载");
}

function downloadMaterial() {
  materialDownloaded.value = true;
  showToast("资料已下载");
}

function downloadReport() {
  hasDownloaded.value = true;
  showToast("已开始下载提分宝");
}
</script>
