import axios from 'axios'

export async function getHealth(): Promise<any> {
  const res = await axios.get('/api/health')
  return res.data
}

