import Taro from '@tarojs/taro';
import { View, Text, ScrollView } from '@tarojs/components';
import './index.scss';

export default function PrivacyPolicyPage() {
  return (
    <ScrollView className="page" scrollY>
      <View className="agreement-content">
        <Text className="title">隐私政策</Text>
        <Text className="update-date">更新日期：2026年6月2日</Text>

        <View className="section">
          <Text className="section-title">一、我们收集的信息</Text>
          <Text className="paragraph">
            1.1 注册信息：当您通过微信授权登录时，我们会获取您的微信 OpenID、昵称和头像信息，用于创建和识别您的账号。
          </Text>
          <Text className="paragraph">
            1.2 使用信息：您使用 AI 工具时输入的提示词、商品描述、待翻译文本等，以及平台生成的 AI 结果内容。
          </Text>
          <Text className="paragraph">
            1.3 交易信息：您的充值记录、消费记录、Token 余额等财务相关信息。
          </Text>
          <Text className="paragraph">
            1.4 设备信息：我们可能收集您的设备型号、操作系统版本等基础信息，用于优化服务体验。
          </Text>
        </View>

        <View className="section">
          <Text className="section-title">二、信息的使用</Text>
          <Text className="paragraph">
            2.1 为您提供 AI 工具服务：使用您输入的内容调用 AI 模型生成结果。
          </Text>
          <Text className="paragraph">
            2.2 账号管理：维护您的用户账号、工作室信息和权限。
          </Text>
          <Text className="paragraph">
            2.3 计费结算：记录 Token 消耗和充值，确保计费准确。
          </Text>
          <Text className="paragraph">
            2.4 服务改进：通过使用数据分析优化产品功能和服务质量。
          </Text>
          <Text className="paragraph">
            2.5 通知推送：在余额不足、充值成功等场景下向您发送订阅消息通知。
          </Text>
        </View>

        <View className="section">
          <Text className="section-title">三、信息的存储</Text>
          <Text className="paragraph">
            3.1 您的信息存储在中华人民共和国境内的服务器上，不会传输至境外。
          </Text>
          <Text className="paragraph">
            3.2 我们采用业界通用的安全技术措施保护您的信息安全，包括数据加密、访问控制等。
          </Text>
          <Text className="paragraph">
            3.3 在满足服务目的所必需的最短期限后，我们将删除或匿名化您的个人信息。
          </Text>
        </View>

        <View className="section">
          <Text className="section-title">四、信息的共享</Text>
          <Text className="paragraph">
            4.1 未经您的同意，我们不会向第三方共享您的个人信息，但以下情况除外：
          </Text>
          <Text className="paragraph">
            (1) 遵守法律法规及政府主管部门的强制性要求；
          </Text>
          <Text className="paragraph">
            (2) 与关联公司共享：仅为实现服务功能所必需的范围；
          </Text>
          <Text className="paragraph">
            (3) 与授权合作伙伴共享：仅为实现服务功能所必需，且受严格保密约束。
          </Text>
        </View>

        <View className="section">
          <Text className="section-title">五、您的权利</Text>
          <Text className="paragraph">
            5.1 查询和更正：您可以在账户管理页面查询和更正您的个人信息。
          </Text>
          <Text className="paragraph">
            5.2 删除：在您注销账号后，我们将删除您的个人信息或进行匿名化处理。
          </Text>
          <Text className="paragraph">
            5.3 撤回同意：您可以关闭订阅消息通知，我们将不再向您推送相关通知。
          </Text>
        </View>

        <View className="section">
          <Text className="section-title">六、未成年人保护</Text>
          <Text className="paragraph">
            6.1 本平台主要面向毛衫行业从业者，不面向未成年人提供服务。
          </Text>
          <Text className="paragraph">
            6.2 如我们发现在未获得监护人同意的情况下收集了未成年人信息，将尽快删除相关数据。
          </Text>
        </View>

        <View className="section">
          <Text className="section-title">七、隐私政策的更新</Text>
          <Text className="paragraph">
            7.1 我们可能适时修订本隐私政策，修订后的政策将在本平台公布。
          </Text>
          <Text className="paragraph">
            7.2 对于重大变更，我们将通过弹窗、站内信等方式通知您。
          </Text>
        </View>

        <View className="section">
          <Text className="section-title">八、联系我们</Text>
          <Text className="paragraph">
            如您对本隐私政策有任何疑问、意见或建议，可通过本平台内客服功能或官方邮箱与我们联系。我们将在15个工作日内回复您的请求。
          </Text>
        </View>
      </View>
    </ScrollView>
  );
}