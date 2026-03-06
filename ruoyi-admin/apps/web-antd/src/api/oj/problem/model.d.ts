export interface ProblemCategory {
  id: number;
  name: string;
}

export interface Problem {
  id: number;
  title: string;
  description: string;
  difficulty: number;
  timeLimit: number;
  memoryLimit: number;
  submitCount: number;
  acCount: number;
  status: number;
  createdAt: string;
  updatedAt: string;
  categories: ProblemCategory[];
  categoryIds?: number[];
}
