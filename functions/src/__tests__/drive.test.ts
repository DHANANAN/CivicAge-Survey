import { generateSummaryCsv, withExponentialBackoff } from '../index';

describe('Cloud Function Drive Integration Unit Tests', () => {
  describe('generateSummaryCsv', () => {
    it('should correctly format key-value payload into a CSV string', () => {
      const samplePayload = {
        A1_age: '78',
        A2_gender: 'Female',
        B1_phone: 'Smartphone',
      };

      const csv = generateSummaryCsv(samplePayload);

      expect(csv).toContain('Question ID,Question Prompt,Response Value');
      expect(csv).toContain('"A1_age","A1_age","78"');
      expect(csv).toContain('"A2_gender","A2_gender","Female"');
      expect(csv).toContain('"B1_phone","B1_phone","Smartphone"');
    });

    it('should sanitize double quotes inside values', () => {
      const payload = {
        notes: 'User said "Yes" to option',
      };
      const csv = generateSummaryCsv(payload);
      expect(csv).toContain('"User said ""Yes"" to option"');
    });
  });

  describe('withExponentialBackoff', () => {
    it('should return result on first successful attempt', async () => {
      const mockFn = jest.fn().mockResolvedValue('success_data');
      const result = await withExponentialBackoff(mockFn, 3, 10);

      expect(result).toBe('success_data');
      expect(mockFn).toHaveBeenCalledTimes(1);
    });

    it('should retry on 429 rate limit error and succeed', async () => {
      const mockFn = jest
        .fn()
        .mockRejectedValueOnce({ response: { status: 429 } })
        .mockResolvedValueOnce('retry_success');

      const result = await withExponentialBackoff(mockFn, 3, 10);
      expect(result).toBe('retry_success');
      expect(mockFn).toHaveBeenCalledTimes(2);
    });

    it('should throw after max retries exceeded', async () => {
      const mockFn = jest.fn().mockRejectedValue({ response: { status: 500 } });
      await expect(withExponentialBackoff(mockFn, 2, 10)).rejects.toThrow();
      expect(mockFn).toHaveBeenCalledTimes(2);
    });
  });
});
