import Taro from '@tarojs/taro';
import { View, Text, ScrollView } from '@tarojs/components';
import './index.scss';

export default function UserAgreementPage() {
  return (
    <ScrollView className="page" scrollY>
      <View className="agreement-content">
        <Text className="title">用户服务协议</Text>
        <Text className="update-date">更新日期：2026年6月2日</Text>

        <View className="section">
          <Text className="section-title">一、总则</Text>
          <Text className="paragraph">
            1.1 本协议是您与濮院毛衫 AI 平台（以下简称"本平台"）之间关于使用本平台服务所订立的协议。"本平台"是指由濮院毛衫 AI 平台运营的，面向毛衫行业提供 AI 工具服务的在线平台。
          </Text>
          <Text className="paragraph">
            1.2 您在使用本平台服务前，应当认真阅读本协议。一旦您使用本平台服务，即视为您已充分理解并同意本协议的全部内容。
          </Text>
        </View>

        <View className="section">
          <Text className="section-title">二、服务内容</Text>
          <Text className="paragraph">
            2.1 本平台提供 AI 图片生成、AI 脚本生成、AI 跨境翻译等智能工具服务，旨在帮助毛衫行业从业者提升工作效率。
          </Text>
          <Text className="paragraph">
            2.2 本平台采用 Token 计费模式，用户通过充值获取 Token，使用各功能时按消耗量扣减 Token。
          </Text>
          <Text className="paragraph">
            2.3 本平台有权根据业务发展需要，增加、修改或终止部分服务内容，并将提前通知用户。
          </Text>
        </View>

        <View className="section">
          <Text className="section-title">三、用户账号</Text>
          <Text className="paragraph">
            3.1 用户通过微信授权方式注册账号，注册后获得个人工作室及相应角色权限。
          </Text>
          <Text className="paragraph">
            3.2 用户应妥善保管账号信息，因用户自身原因导致账号泄露、被盗等造成的损失，由用户自行承担。
          </Text>
          <Text className="paragraph">
            3.3 用户不得将账号转让、出售或出借给他人使用。
          </Text>
        </View>

        <View className="section">
          <Text className="section-title">四、充值与退款</Text>
          <Text className="paragraph">
            4.1 用户可通过微信支付进行充值，充值成功后 Token 即时到账。
          </Text>
          <Text className="paragraph">
            4.2 Token 一经充值成功，非因本平台系统故障导致的充值错误，不予退款。
          </Text>
          <Text className="paragraph">
            4.3 如因系统故障导致充值金额异常，用户应及时联系客服处理。
          </Text>
        </View>

        <View className="section">
          <Text className="section-title">五、使用规范</Text>
          <Text className="paragraph">
            5.1 用户使用本平台服务时，应遵守中华人民共和国相关法律法规。
          </Text>
          <Text className="paragraph">
            5.2 用户不得利用本平台生成违反法律法规、侵害他人权益的内容。
          </Text>
          <Text className="paragraph">
            5.3 用户不得通过任何手段恶意攻击本平台系统、刷取 Token 或进行其他违规操作。
          </Text>
        </View>

        <View className="section">
          <Text className="section-title">六、免责声明</Text>
          <Text className="paragraph">
            6.1 AI 生成内容由人工智能模型产出，本平台不对生成内容的准确性、完整性、适用性作出保证。
          </Text>
          <Text className="paragraph">
            6.2 因不可抗力、系统维护等原因导致服务中断，本平台不承担由此造成的损失。
          </Text>
          <Text className="paragraph">
            6.3 用户应自行判断 AI 生成内容的适用性，并对使用该内容所产生的后果自行负责。
          </Text>
        </View>

        <View className="section">
          <Text className="section-title">七、知识产权</Text>
          <Text className="paragraph">
            7.1 本平台的所有技术架构、界面设计、品牌标识等均受知识产权法律保护。
          </Text>
          <Text className="paragraph">
            7.2 用户使用本平台生成的图片、脚本等内容，用户享有使用权，但不得声明为本平台的原创作品。
          </Text>
        </View>

        <View className="section">
          <Text className="section-title">八、协议修改</Text>
          <Text className="paragraph">
            8.1 本平台有权根据需要修改本协议内容，修改后的协议将在本平台公布。
          </Text>
          <Text className="paragraph">
            8.2 如用户不同意修改后的协议，可选择停止使用本平台服务；继续使用则视为接受修改后的协议。
          </Text>
        </View>

        <View className="section">
          <Text className="section-title">九、联系方式</Text>
          <Text className="paragraph">
            如您对本协议有任何疑问，可通过本平台内客服功能或官方邮箱联系我们。
          </Text>
        </View>
      </View>
    </ScrollView>
  );
}