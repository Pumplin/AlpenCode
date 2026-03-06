export interface TestCase {
  id: number;
  problemId: number;
  input: string;
  expectedOutput: string;
  isSample: number; // 0=隐藏 1=公开
  sort: number;
  status: number;
  createdAt: string;
  updatedAt: string;
}
