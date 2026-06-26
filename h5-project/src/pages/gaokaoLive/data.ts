import bottomImage from "./assets/bottom.jpg";
import seniorThreeQrCodeImage from "./assets/qrcode-2.jpg";
import seniorTwoQrCodeImage from "./assets/qrcode-1.jpg";
import topImage from "./assets/top.png";

export type GradeId = "senior-two" | "senior-three";

export interface Course {
  subject: string;
  date: string;
  times: string[];
  teacher: string;
  topics: string[];
}

export interface GradePage {
  id: GradeId;
  label: string;
  courses: Course[];
  bottomImage: string;
  benefits: string[];
  qrImage: string;
  qrTitle: string;
  reserveUrl: string;
}

export { topImage };

const groupBenefits = [
  "领取配套讲义习题",
  "更多高中精讲课程",
  "智能作业批改服务",
  "高考大招状元笔记",
];

export const gradePages: GradePage[] = [
  {
    id: "senior-two",
    label: "高二年级",
    bottomImage,
    benefits: groupBenefits,
    qrImage: seniorTwoQrCodeImage,
    qrTitle: "高二学习交流答疑群",
    reserveUrl: "https://jxea4.xetslk.com/s/11r79g",
    courses: [
      {
        subject: "数学",
        date: "6月6日",
        times: ["16:00-17:00", "15:00-16:00"],
        teacher: "林观云/景肖龙",
        topics: ["高二跃升:夯实圆的标准与一般方程基础", "高二培优:极化恒等式"],
      },
      {
        subject: "物理",
        date: "6月7日",
        times: ["19:00-20:00"],
        teacher: "潘一铭",
        topics: ["高二:电势能和电势"],
      },
      {
        subject: "英语",
        date: "6月7日",
        times: ["15:00-16:00"],
        teacher: "刘献瑶",
        topics: ["高二:完形填空--动词名词专项"],
      },
      {
        subject: "语文",
        date: "6月6日",
        times: ["19:00-20:00"],
        teacher: "张亚柔",
        topics: ["语文专题:古代诗歌鉴赏(适用高二高三)"],
      },
    ],
  },
  {
    id: "senior-three",
    label: "高三年级",
    bottomImage,
    benefits: groupBenefits,
    qrImage: seniorThreeQrCodeImage,
    qrTitle: "高三学习交流答疑群",
    reserveUrl: "https://jxea4.xetslk.com/s/4b4nSx",
    courses: [
      {
        subject: "数学",
        date: "6月6日",
        times: ["16:00-17:00", "16:00-17:00"],
        teacher: "林观云/侯杰",
        topics: ["高三跃升:函数的对称性", "高三培优:圆与直线在高考中的应用"],
      },
      {
        subject: "物理",
        date: "6月7日",
        times: ["19:00-20:00"],
        teacher: "王天宇",
        topics: ["高三:牛顿第二定律中的供需关系"],
      },
      {
        subject: "英语",
        date: "6月7日",
        times: ["16:00-17:00"],
        teacher: "刘献瑶",
        topics: ["高三:读后续写--情绪升华"],
      },
      {
        subject: "语文",
        date: "6月6日",
        times: ["19:00-20:00"],
        teacher: "张亚柔",
        topics: ["语文专题:古代诗歌鉴赏(适用高二高三)"],
      },
    ],
  },
];
