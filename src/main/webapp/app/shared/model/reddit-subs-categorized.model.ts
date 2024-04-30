export interface IRedditSubsCategorized {
  id?: number;
  sub?: string;
  cat?: string;
  subcat?: string;
  niche?: string;
}

export const defaultValue: Readonly<IRedditSubsCategorized> = {};
